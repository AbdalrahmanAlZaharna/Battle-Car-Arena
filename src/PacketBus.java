import java.util.concurrent.CopyOnWriteArrayList;

public class PacketBus {
    private final CopyOnWriteArrayList<PacketListener> ls = new CopyOnWriteArrayList<>();

    public void add(PacketListener l) {
        ls.add(l);
    }

    public void remove(PacketListener l) {
        ls.remove(l);
    }

    public void publish(Packet p) {
        for (var l : ls) {
            try {
                l.onPacket(p);
            } catch (Throwable t) {
                System.err.println("Error in PacketListener " +
                        l.getClass().getSimpleName() + ":");
                t.printStackTrace();
            }
        }
    }
}
