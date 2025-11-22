public record Packet(int team, int flags, int value) {
    public boolean ir()    { return (flags & 0x01) != 0; } // FLAG_IR
    public boolean ldr()   { return (flags & 0x02) != 0; } // FLAG_LDR (edge, optional)
    public boolean light() { return (flags & 0x04) != 0; } // FLAG_LIGHT (bright, continuous)
}
