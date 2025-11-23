import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * Controls the physical USB port using the JSSC library.
 * It opens the connection and handles reading/writing bytes.
 */
public class SerialPortHandle implements AutoCloseable {
    private final String portName;
    private final int baud;
    private SerialPort port;

    public SerialPortHandle(String portName, int baud) {
        if (portName == null || portName.isEmpty()) {
            throw new IllegalArgumentException("portName must not be empty");
        }
        this.portName = portName;
        this.baud = baud;
    }

    /**
     * Connects to the USB port.
     * Sets the speed to 9600, with 8 data bits, 1 stop bit, and no parity.
     * These are the standard settings for Arduino XBee communication.
     */
    public void open() {
        port = new SerialPort(portName);
        try {
            if (!port.openPort()) {
                throw new RuntimeException("Failed to open " + portName);
            }
            port.setParams(
                    baud,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE
            );
            port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            System.out.println("Opened " + portName + " @ " + baud + " 8N1");
        } catch (SerialPortException e) {
            throw new RuntimeException("Open error on " + portName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads data from the USB port into the provided buffer.
     * @param buf The array where we will store the data we read.
     * @param len The maximum number of bytes to read.
     * @return The actual number of bytes we read (0 if nothing was there).
     */
    public int read(byte[] buf, int len) {
        if (port == null) return -1;
        if (buf == null || len <= 0) return 0;
        try {
            int available = port.getInputBufferBytesCount();
            if (available <= 0) {
                // If no data is waiting, sleep for 10ms to save CPU power
                try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                available = port.getInputBufferBytesCount();
                if (available <= 0) return 0;
            }
            int toRead = Math.min(available, len);
            byte[] data = port.readBytes(toRead);
            if (data == null) return 0;
            System.arraycopy(data, 0, buf, 0, data.length);
            return data.length;
        } catch (SerialPortException e) {
            throw new RuntimeException("Read error on " + portName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Sends bytes out to the USB port.
     * @param buf The array of bytes to send.
     */
    public synchronized void write(byte[] buf) {
        if (port == null) throw new IllegalStateException("Port not open");
        if (buf == null || buf.length == 0) return;
        try {
            port.writeBytes(buf);
        } catch (SerialPortException e) {
            throw new RuntimeException("Write error on " + portName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the connection is currently active.
     * @return true if connected, false if closed.
     */
    public boolean isOpen() {
        return port != null && port.isOpened();
    }

    /**
     * Disconnects from the USB port.
     * This allows other programs to use the port.
     */
    @Override
    public void close() {
        if (port != null) {
            try {
                port.closePort();
            } catch (SerialPortException ignored) {}
            port = null;
        }
    }

    /**
     * Gets a list of all available USB serial ports on the computer.
     * @return An array of port names (e.g., "COM3", "COM4").
     */
    public static String[] listPorts() {
        return SerialPortList.getPortNames();
    }

    @Override
    public String toString() {
        return "SerialPortHandle(" + portName + ")";
    }
}