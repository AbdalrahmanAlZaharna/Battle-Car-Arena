import java.util.concurrent.BlockingQueue;

public class SerialReader implements Runnable {
    private final SerialEndpoint serial;
    private final BlockingQueue<byte[]> outQ;

    public SerialReader(SerialEndpoint serial, BlockingQueue<byte[]> outQ){
        this.serial = serial;
        this.outQ = outQ;
    }

    @Override
    public void run() {
        byte[] buf = new byte[4];
        try {
            while(true){
                int n = serial.read(buf);
                if(n == 4){
                    byte[] frame = new byte[4];
                    System.arraycopy(buf, 0, frame, 0, 4);
                    outQ.put(frame);
                }
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
