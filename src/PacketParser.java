import java.util.Optional;

public class PacketParser {
    private final ChecksumStrategy sum;
    public PacketParser(ChecksumStrategy sum){ this.sum = sum; }

    public Optional<Packet> parse(byte[] raw){
        if(raw == null || raw.length != 4) return Optional.empty();
        if(!sum.valid(raw[0], raw[1], raw[2], raw[3])) return Optional.empty();
        int team  = raw[0] & 0xFF;
        int flags = raw[1] & 0xFF;
        int value = raw[2] & 0xFF;
        return Optional.of(new Packet(team, flags, value));
    }
}
