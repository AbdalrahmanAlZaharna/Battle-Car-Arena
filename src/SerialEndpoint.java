/**
 * A simplified interface for using the Serial Port.
 * This acts like a "Adapter" or "Bridge".
 * It makes it easier for the rest of the app to talk to the hardware
 * without worrying about the complex details of the library (JSSC).
 */
public class SerialEndpoint implements AutoCloseable {
    private final SerialPortHandle handle;
    private final byte[] oneByte = new byte[1];

    /**
     * Constructor: Connects to the specific port (like "COM3") at a specific speed.
     * It immediately opens the connection.
     */
    public SerialEndpoint(String name, int baud) {
        this.handle = new SerialPortHandle(name, baud);
        this.handle.open();
    }

    /**
     * Reads a bunch of bytes at once.
     * @param buf The container to fill with data.
     * @return The number of bytes we actually got.
     */
    public int read(byte[] buf) {
        if (buf == null || buf.length == 0) return 0;
        return handle.read(buf, buf.length);
    }

    /**
     * Reads exactly ONE byte.
     * This is useful for the Reader thread that looks at data one-by-one.
     * @return The byte value (0-255), or -1 if the buffer is empty.
     */
    public int readOne() {
        int n = handle.read(oneByte, 1);
        // We use & 0xFF to make sure it's a positive number
        if (n == 1) return oneByte[0] & 0xFF;
        return -1;
    }

    /**
     * Sends data out to the hardware.
     * @param buf The bytes to send.
     */
    public synchronized void write(byte[] buf) {
        handle.write(buf);
    }

    /**
     * Checks if the connection is alive.
     */
    public boolean isOpen() { return handle.isOpen(); }

    /**
     * Closes the connection properly so we don't crash the computer's USB driver.
     */
    @Override
    public void close() { handle.close(); }

    @Override
    public String toString() { return "SerialEndpoint(" + handle + ")"; }

    /**
     * Asks the computer for a list of all plugged-in USB devices.
     */
    public static String[] listPorts() {
        return SerialPortHandle.listPorts();
    }
}