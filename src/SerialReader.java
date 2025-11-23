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
        byte[] one = new byte[1];
        byte[] frame = new byte[4];
        int idx = 0;
        try {
            while(true){
                int n = serial.read(one);
                if (n <= 0) continue;

                frame[idx++] = one[0];
                if (idx == 4) {
                    byte[] out = new byte[4];
                    System.arraycopy(frame, 0, out, 0, 4);
                    outQ.put(out);
                    idx = 0;
                }
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
