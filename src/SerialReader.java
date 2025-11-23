import java.util.concurrent.BlockingQueue;

/**
 * A background task that reads raw bytes from the USB.
 * It groups them into sets of 4 bytes and puts them in a queue for processing.
 */
public class SerialReader implements Runnable {
    private final SerialEndpoint serial;
    private final BlockingQueue<byte[]> outQ;

    public SerialReader(SerialEndpoint serial, BlockingQueue<byte[]> outQ){
        this.serial = serial;
        this.outQ = outQ;
    }

    /**
     * The main loop for this thread.
     * 1. Reads one byte at a time.
     * 2. Adds it to a temporary frame array.
     * 3. When 4 bytes are collected, sends the frame to the output queue.
     */
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
                
                // Once we have 4 bytes, create a new array and send it
                if (idx == 4) {
                    byte[] out = new byte[4];
                    System.arraycopy(frame, 0, out, 0, 4);
                    outQ.put(out); // Puts the data in the queue
                    idx = 0;       // Reset index for the next packet
                }
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}