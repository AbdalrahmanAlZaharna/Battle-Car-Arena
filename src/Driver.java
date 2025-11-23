import jssc.SerialPortList;
import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The starting point of the application.
 * This class sets up the hardware, connects the logic, and starts the background tasks.
 */
public class Driver {

    /**
     * Connects the Game Engine to the Serial Port.
     */
    private static class HandleCommandDispatcher extends CommandDispatcher {
        private final SerialPortHandle handle;

        public HandleCommandDispatcher(SerialPortHandle handle) {
            super(null); 
            this.handle = handle;
        }

        @Override
        public synchronized void send(Command c) {
            if (handle == null || !handle.isOpen()) return;
            byte[] frame = c.toBytes();
            handle.write(frame);
        }
    }

    public static void main(String[] args) {
        // Handle background errors
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught in thread " + t.getName() + ":");
            e.printStackTrace();
        });

        // 1. List available serial ports
        System.out.println("Available serial ports:");
        String[] ports = SerialPortList.getPortNames();
        for (String p : ports) System.out.println("  " + p);
        
        // 2. Select port
        String preferred = System.getProperty("port", ports.length > 0 ? ports[0] : "COM7");
        String portName = choosePortOrFallback(preferred, ports);
        int baud = 9600;

        System.out.println("Using ZigBee dongle on " + portName);

        // 3. Open Serial Port
        SerialPortHandle handle = new SerialPortHandle(portName, baud);
        handle.open();   // open the port only once here

        // Create a SerialEndpoint that reuses the same opened handle
        SerialEndpoint endpoint = new SerialEndpoint(handle);

        // 4. Initialize Components
        BlockingQueue<byte[]> inQ = new LinkedBlockingQueue<>(512);
        ChecksumStrategy checksum = new SumModuloChecksum();
        PacketParser parser = new PacketParser(checksum);
        PacketBus bus = new PacketBus();

        // 5. Initialize Engine and UI
        CommandDispatcher dispatcher = new HandleCommandDispatcher(handle);
        ScoreboardUI ui = new ScoreboardUI();
        GameEngine engine = new GameEngine(dispatcher);
        engine.addListener(ui);

        // 6. Debug Logger (Prints received packets to console)
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

        bus.add(engine);

        // 7. Show UI
        SwingUtilities.invokeLater(() -> ui.setVisible(true));

        // 8. Start Background Threads
        Thread reader = new Thread(new SerialReader(endpoint, inQ), "Reader"); 
        Thread decoder = new Thread(new PacketDecoder(inQ, parser, bus), "Decoder");
        Thread engineThread = new Thread(engine::runLoop, "Engine");

        reader.start();
        decoder.start();
        engineThread.start();

        // 9. Close port when program ends
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { endpoint.close(); } catch (Exception ignored) {}
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
