public class CommandDispatcher {
    protected final SerialEndpoint serial; // not used in multi, but kept for API
    public CommandDispatcher(SerialEndpoint serial){ this.serial = serial; }
    public void send(Command c){
        if(serial == null) return;
        serial.write(c.toBytes());
    }
    public void setRgbAll(int code){
        send(Commands.setRgb(1, code));
        send(Commands.setRgb(2, code));
    }
}
