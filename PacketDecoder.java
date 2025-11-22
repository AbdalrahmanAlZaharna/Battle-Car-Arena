import java.util.concurrent.BlockingQueue;

public class PacketDecoder implements Runnable {
    private final BlockingQueue<byte[]> inQ;
    private final PacketParser parser;
    private final PacketBus bus;

    public PacketDecoder(BlockingQueue<byte[]> inQ, PacketParser parser, PacketBus bus){
        this.inQ = inQ;
        this.parser = parser;
        this.bus = bus;
    }

    @Override
    public void run() {
        try {
            while(true){
                byte[] f = inQ.take();
                parser.parse(f).ifPresent(bus::publish);
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
