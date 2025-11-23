/**
 * Interface representing a 32-bit (4-byte) command packet.
 * * EXACT PACKET STRUCTURE (4 Bytes):
 * * Byte [0]: TEAM ID
 * -------------------------------------------------------------
 * 00000001 (0x01) -> Team 1
 * 00000010 (0x02) -> Team 2
 * 11111111 (0xFF) -> TEAM_ALL (Broadcast)
 * * Byte [1]: COMMAND ID
 * -------------------------------------------------------------
 * 00000001 (0x01) -> CMD_SET_RGB (Change LED Color)
 * 00000010 (0x02) -> CMD_BEEP    (Play Sound)
 * 00000011 (0x03) -> CMD_FIREMODE (Safety/Semi/Auto)
 * * Byte [2]: ARGUMENT (Depends on Command ID)
 * -------------------------------------------------------------
 * IF CMD_SET_RGB:
 * 00000000 (0) -> OFF
 * 00000001 (1) -> GREEN
 * 00000010 (2) -> YELLOW (or BLUE for Team 2)
 * 00000011 (3) -> RED
 * * IF CMD_BEEP:
 * 00000001 (1) -> Short Beep (Hit)
 * 00000010 (2) -> Long Beep (Die)
 * 00000011 (3) -> 3x Chirp (Start)
 * * IF CMD_FIREMODE:
 * 00000000 (0) -> Safety On (Disabled)
 * 00000010 (2) -> Auto Fire (Enabled)
 * * Byte [3]: CHECKSUM
 * -------------------------------------------------------------
 * Sum of Bytes [0] + [1] + [2] (Modulo 256).
 * Used by Arduino to verify data integrity.
 */
public interface Command {
    /**
     * Serializes the command into a byte array for transmission.
     * @return A byte array of length 4 containing [Team, Cmd, Arg, Sum].
     */
    byte[] toBytes(); 
}