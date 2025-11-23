import jssc.SerialPortList;
import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Battle Car Driver using SerialPortHandle (jSSC 2.9.6)
 * - Opens one COM port (your XBee USB dongle)
 * - Starts:
 *      * Serial reader thread (reads 4-byte frames)
 *      * Packet decoder thread (parses packets and pushes to PacketBus)
 *      * GameEngine loop
 *      * Scoreboard UI
 * - Logs every received packet with flags decoded
 */
public class Driver {

    /**
     * Command dispatcher that sends commands to the cars
     * using SerialPortHandle instead of SerialEndpoint.
     */
    private static class HandleCommandDispatcher extends CommandDispatcher {
        private final SerialPortHandle handle;

        public HandleCommandDispatcher(SerialPortHandle handle) {
            super(null);            // we don't use the old SerialEndpoint
            this.handle = handle;
        }

        @Override
        public synchronized void send(Command c) {
            if (handle == null || !handle.isOpen()) return;
            byte[] frame = c.toBytes();
            handle.write(frame);
        }
    }

    /**
     * Serial reader that uses SerialPortHandle and assembles 4-byte frames.
     * Each frame is put into the BlockingQueue for PacketDecoder.
     */
    private static class HandleSerialReader implements Runnable {
        private final SerialPortHandle handle;
        private final BlockingQueue<byte[]> outQ;

        public HandleSerialReader(SerialPortHandle handle, BlockingQueue<byte[]> outQ) {
            this.handle = handle;
            this.outQ = outQ;
        }

        @Override
        public void run() {
            byte[] one = new byte[1];
            byte[] frame = new byte[4];
            int idx = 0;

            try {
                while (true) {
                    int n = handle.read(one, 1);
                    if (n <= 0) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                    frame[idx++] = one[0];
                    if (idx == 4) {
                        byte[] out = Arrays.copyOf(frame, 4);
                        outQ.put(out);
                        idx = 0;
                    }
                }
            } catch (Exception e) {
                System.err.println("Reader thread error: " + e);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        // Global handler so background thread crashes are visible
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught in thread " + t.getName() + ":");
            e.printStackTrace();
        });

        // 1) List ports so you see what exists
        System.out.println("Available serial ports:");
        String[] ports = SerialPortList.getPortNames();
        for (String p : ports) System.out.println("  " + p);

        // 2) Choose port: -Dport=COM7 or default to first available
        String preferred = System.getProperty("port", ports.length > 0 ? ports[0] : "COM7");
        String portName = choosePortOrFallback(preferred, ports);
        int baud = 9600;

        System.out.println("Using ZigBee dongle on " + portName);

        // 3) Open port using SerialPortHandle (jSSC)
        SerialPortHandle handle = new SerialPortHandle(portName, baud);
        handle.open();   // prints "Opened COMx @ 9600 8N1"

        // 4) Queues and parser/bus
        BlockingQueue<byte[]> inQ = new LinkedBlockingQueue<>(512);
        ChecksumStrategy checksum = new SumModuloChecksum();
        PacketParser parser = new PacketParser(checksum);
        PacketBus bus = new PacketBus();

        // 5) Command dispatcher that sends to cars via this handle
        CommandDispatcher dispatcher = new HandleCommandDispatcher(handle);

        // 6) Game engine + UI
        ScoreboardUI ui = new ScoreboardUI();
        GameEngine engine = new GameEngine(dispatcher);
        engine.addListener(ui);

        // 7) Add a debug listener to print every packet with flags decoded
        bus.add(p -> {
            String bits = String.format("%4s", Integer.toBinaryString(p.flags() & 0xF))
                    .replace(' ', '0');
            System.out.println(
                    "RX packet: team=" + p.team() +
                    " flags=" + bits +
                    " [IR=" + p.ir() +
                    ", LDR=" + p.ldr() +
                    ", LIGHT=" + p.light() +
                    "] value=" + p.value()
            );
        });

        // GameEngine also listens to packets
        bus.add(engine);

        // 8) Start UI
        SwingUtilities.invokeLater(() -> ui.setVisible(true));

        // 9) Start threads: reader, decoder, engine loop
        Thread reader = new Thread(new HandleSerialReader(handle, inQ), "Reader");
        Thread decoder = new Thread(new PacketDecoder(inQ, parser, bus), "Decoder");
        Thread engineThread = new Thread(engine::runLoop, "Engine");

        reader.start();
        decoder.start();
        engineThread.start();

        // 10) Close serial port on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { handle.close(); } catch (Exception ignored) {}
            System.out.println("Serial port closed.");
        }));

        try {
            engineThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String choosePortOrFallback(String preferred, String[] ports) {
        for (String p : ports) if (p.equalsIgnoreCase(preferred)) return preferred;
        return ports.length > 0 ? ports[0] : preferred;
    }
}
