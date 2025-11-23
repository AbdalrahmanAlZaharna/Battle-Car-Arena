// SerialEndpoint.java
// Thin wrapper around SerialPortHandle so everything else uses this.

public class SerialEndpoint implements AutoCloseable {
    private final SerialPortHandle handle;
    private final byte[] oneByte = new byte[1];

    public SerialEndpoint(String name, int baud) {
        this.handle = new SerialPortHandle(name, baud);
        this.handle.open(); // prints "Opened COMx @ 9600 8N1" once
    }

    /** Reads up to buf.length bytes, returns number of bytes read (0..buf.length). */
    public int read(byte[] buf) {
        if (buf == null || buf.length == 0) return 0;
        return handle.read(buf, buf.length);
    }

    /** Convenience: read exactly one byte, returns -1 if none available. */
    public int readOne() {
        int n = handle.read(oneByte, 1);
        if (n == 1) return oneByte[0] & 0xFF;
        return -1;
    }

    public synchronized void write(byte[] buf) {
        handle.write(buf);
    }

    public boolean isOpen() { return handle.isOpen(); }

    @Override
    public void close() { handle.close(); }

    @Override
    public String toString() { return "SerialEndpoint(" + handle + ")"; }

    public static String[] listPorts() {
        return SerialPortHandle.listPorts();
    }
}
