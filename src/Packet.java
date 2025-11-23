/**
 * Immutable data carrier representing a packet received from the hardware.
 * Decodes bit flags into boolean states.
 */
public record Packet(int team, int flags, int value) {
    /**
     * Checks Bit 0 (0x01)
     * @return true if IR signal is detected (Hit).
     */
    public boolean ir()    { return (flags & 0x01) != 0; } 

    /**
     * Checks Bit 1 (0x02)
     * Debugging flag for LDR edge detection.
     */
    public boolean ldr()   { return (flags & 0x02) != 0; } 

    /**
     * Checks Bit 2 (0x04)
     * @return true if sensor is detecting light (Bright), false if Covered (Dark).
     */
    public boolean light() { return (flags & 0x04) != 0; } 
}