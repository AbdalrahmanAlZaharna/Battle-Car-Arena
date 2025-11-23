/**
 * This class handles sending messages to the hardware.
 * It keeps a link to the USB connection (SerialEndpoint) and 
 * converts our Command objects into bytes to send them.
 */
public class CommandDispatcher {
    // The link to the physical USB connection.
    // It is 'protected' so the Multi version can use it too.
    protected final SerialEndpoint serial;

    /**
     * Constructor.
     * saves the USB connection we want to use.
     */
    public CommandDispatcher(SerialEndpoint serial){
        this.serial = serial;
    }

    /**
     * Turns a Command object into bytes and sends it through the USB.
     */
    public void send(Command c){
        // Safety check: if there is no connection, do nothing.
        if(serial == null) return;
        serial.write(c.toBytes());
    }

    /**
     * A shortcut to change the LED color for BOTH teams at the same time.
     * It sends two separate messages: one for Team 1, one for Team 2.
     * @param code The color number (0=Off, 1=Green, 2=Yellow, 3=Red).
     */
    public void setRgbAll(int code){
        send(Commands.setRgb(1, code));
        send(Commands.setRgb(2, code));
    }
}