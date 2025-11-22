public class CommandDispatcherMulti extends CommandDispatcher {
    private final SerialEndpoint port1, port2;

    public CommandDispatcherMulti(SerialEndpoint p1, SerialEndpoint p2){
        super(null);
        this.port1 = p1;
        this.port2 = p2;
    }

    @Override
    public synchronized void send(Command c){
        byte[] f = c.toBytes();
        int team = f[0] & 0xFF;
        if(team == 1) port1.write(f);
        else if(team == 2) port2.write(f);
        else { port1.write(f); port2.write(f); }
    }
}
