import java.util.concurrent.*;
import javax.swing.*;


public class Driver {
    public static void main(String[] args) {
        BlockingQueue<byte[]> inQ = new LinkedBlockingQueue<>(512);

        ChecksumStrategy checksum = new SumModuloChecksum();
        PacketParser parser = new PacketParser(checksum);
        PacketBus bus = new PacketBus();

        SerialEndpoint port1 = new SerialEndpoint(System.getProperty("port1", "COM3"), 9600);
        SerialEndpoint port2 = new SerialEndpoint(System.getProperty("port2", "COM4"), 9600);

        CommandDispatcher dispatcher = new CommandDispatcherMulti(port1, port2);

        ScoreboardUI ui = new ScoreboardUI();
        GameEngine engine = new GameEngine(dispatcher);
        engine.addListener(ui);

        bus.add(engine);

        SwingUtilities.invokeLater(() -> {
            ui.setVisible(true);
        });

        new Thread(new SerialReader(port1, inQ), "Reader-1").start();
        new Thread(new SerialReader(port2, inQ), "Reader-2").start();
        new Thread(new PacketDecoder(inQ, parser, bus), "Decoder").start();
        new Thread(engine::runLoop, "Engine").start();
    }
}
