// CommandDispatcherMulti.java
import java.util.*;
import java.util.stream.*;

public class CommandDispatcherMulti extends CommandDispatcher {
    private final List<SerialEndpoint> ports;

    public CommandDispatcherMulti(SerialEndpoint... endpoints){
        super(null);
        this.ports = Arrays.stream(endpoints)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (this.ports.isEmpty()) throw new IllegalArgumentException("At least one SerialEndpoint is required");
    }

    @Override
    public synchronized void send(Command c){
        byte[] f = c.toBytes();
        int team = f[0] & 0xFF;

        if (team == 0xFF) {
            for (SerialEndpoint p : ports) p.write(f);
            return;
        }
        if (ports.size() == 1) {
            ports.get(0).write(f);
            return;
        }
        int idx = (team == 1) ? 0 : (team == 2 ? 1 : 0);
        if (idx < ports.size()) ports.get(idx).write(f);
        else for (SerialEndpoint p : ports) p.write(f);
    }
}
