// SerialPortHandle.java
// JSSC-based serial wrapper: open, close, read (blocking), write, list ports.

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

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

    /** Blocking read for up to len bytes; returns number of bytes read (0..len). */
    public int read(byte[] buf, int len) {
        if (port == null) return -1;
        if (buf == null || len <= 0) return 0;
        try {
            int available = port.getInputBufferBytesCount();
            if (available <= 0) {
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

    public synchronized void write(byte[] buf) {
        if (port == null) throw new IllegalStateException("Port not open");
        if (buf == null || buf.length == 0) return;
        try {
            port.writeBytes(buf);
        } catch (SerialPortException e) {
            throw new RuntimeException("Write error on " + portName + ": " + e.getMessage(), e);
        }
    }

    public boolean isOpen() {
        return port != null && port.isOpened();
    }

    @Override
    public void close() {
        if (port != null) {
            try {
                port.closePort();
            } catch (SerialPortException ignored) {}
            port = null;
        }
    }

    public static String[] listPorts() {
        return SerialPortList.getPortNames();
    }

    @Override
    public String toString() {
        return "SerialPortHandle(" + portName + ")";
    }
}
