import java.util.concurrent.BlockingQueue;

public class PacketDecoder implements Runnable {
    private final BlockingQueue<byte[]> inQ;
    private final PacketParser parser;
    private final PacketBus bus;

    public PacketDecoder(BlockingQueue<byte[]> inQ, PacketParser parser, PacketBus bus) {
        this.inQ = inQ;
        this.parser = parser;
        this.bus = bus;
    }

    @Override
    public void run() {
        try {
            while (true) {
                byte[] f = inQ.take();
                try {
                    parser.parse(f).ifPresent(bus::publish);
                } catch (Throwable t) {
                    System.err.println("Error while decoding/publishing packet:");
                    t.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("PacketDecoder interrupted, exiting.");
        }
    }
}
