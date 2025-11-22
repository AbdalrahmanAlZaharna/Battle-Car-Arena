import com.fazecast.jSerialComm.SerialPort;

public class SerialEndpoint implements AutoCloseable {
    private final SerialPort port;

    public SerialEndpoint(String name, int baud){
        port = SerialPort.getCommPort(name);
        port.setBaudRate(baud);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 50, 50);
        if(!port.openPort()){
            throw new RuntimeException("Cannot open " + name);
        }
        System.out.println("Opened " + name);
    }

    public int read(byte[] buf){ return port.readBytes(buf, buf.length); }

    public synchronized void write(byte[] buf){ port.writeBytes(buf, buf.length); }

    @Override
    public void close(){ port.closePort(); }
}
