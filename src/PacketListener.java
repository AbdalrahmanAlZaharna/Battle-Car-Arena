/**
 * Interface for any class that wants to receive packets from the hardware.
 */
public interface PacketListener {
    void onPacket(Packet p);
}