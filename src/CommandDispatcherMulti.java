import java.util.*;
import java.util.stream.*;

/**
 * A smarter version of CommandDispatcher.
 * It can handle multiple USB connections at once.
 * It checks the Team ID in the message to decide which USB port to use.
 */
public class CommandDispatcherMulti extends CommandDispatcher {
    // A list of all working USB connections.
    private final List<SerialEndpoint> ports;

    /**
     * Constructor.
     * Allows you to pass in a list of USB connections (endpoints).
     */
    public CommandDispatcherMulti(SerialEndpoint... endpoints){
        // We pass 'null' to the parent class because we manage the list ourselves here.
        super(null);
        
        // Filter out any broken (null) connections and save the good ones.
        this.ports = Arrays.stream(endpoints)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        // Stop the program if no USB connections were provided.
        if (this.ports.isEmpty()) {
            throw new IllegalArgumentException("At least one SerialEndpoint is required");
        }
    }

    /**
     * Sends the command to the correct place.
     * It looks at the Team ID to pick the right USB port.
     */
    @Override
    public synchronized void send(Command c){
        byte[] f = c.toBytes();
        // Get the Team ID number from the first byte.
        int team = f[0] & 0xFF;

        // CASE 1: BROADCAST (Send to everyone)
        // If the ID is 255 (0xFF), send the message to every USB port we have.
        if (team == 0xFF) {
            for (SerialEndpoint p : ports) p.write(f);
            return;
        }

        // CASE 2: SINGLE PORT
        // If we only have one USB stick plugged in, just send it there.
        if (ports.size() == 1) {
            ports.get(0).write(f);
            return;
        }

        // CASE 3: MULTIPLE PORTS (Routing)
        // If Team 1 -> Use Port 0
        // If Team 2 -> Use Port 1
        int idx = (team == 1) ? 0 : (team == 2 ? 1 : 0);
        
        // Check if that port actually exists before sending.
        if (idx < ports.size()) {
            ports.get(idx).write(f);
        } else {
            // Safety: If we can't find the right port, send to everyone.
            for (SerialEndpoint p : ports) p.write(f);
        }
    }
}