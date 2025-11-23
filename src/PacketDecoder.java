import java.util.concurrent.BlockingQueue;

/**
 * A background task that processes the raw bytes from the queue.
 * It uses the Parser to check if the data is valid, then notifies the game.
 */
public class PacketDecoder implements Runnable {
    private final BlockingQueue<byte[]> inQ;
    private final PacketParser parser;
    private final PacketBus bus;

    public PacketDecoder(BlockingQueue<byte[]> inQ, PacketParser parser, PacketBus bus) {
        this.inQ = inQ;
        this.parser = parser;
        this.bus = bus;
    }

    /**
     * The main loop for this thread.
     * 1. Takes a byte array from the queue (waits if empty).
     * 2. Asks the Parser if the data is valid (checks math).
     * 3. If valid, publishes the Packet to the Bus.
     */
    @Override
    public void run() {
        try {
            while (true) {
                // Blocks (waits) here until data arrives in the queue
                byte[] f = inQ.take();
                try {
                    // Parse the bytes and publish if valid
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