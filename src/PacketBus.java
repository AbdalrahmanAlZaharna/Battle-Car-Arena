import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sends received packets to everyone who is listening (like the Game Engine).
 * Thread-safe so multiple parts of the app can read packets at the same time.
 */
public class PacketBus {
    // A list of all classes that want to receive packets
    private final CopyOnWriteArrayList<PacketListener> ls = new CopyOnWriteArrayList<>();

    /**
     * Adds a listener to the list.
     * @param l The class that wants to receive updates.
     */
    public void add(PacketListener l) {
        ls.add(l);
    }

    /**
     * Removes a listener from the list.
     * @param l The class to remove.
     */
    public void remove(PacketListener l) {
        ls.remove(l);
    }

    /**
     * Sends a packet to all registered listeners.
     * @param p The packet to send.
     */
    public void publish(Packet p) {
        for (var l : ls) {
            try {
                l.onPacket(p);
            } catch (Throwable t) {
                // Keep running even if one listener fails
                System.err.println("Error in PacketListener " +
                        l.getClass().getSimpleName() + ":");
                t.printStackTrace();
            }
        }
    }
}