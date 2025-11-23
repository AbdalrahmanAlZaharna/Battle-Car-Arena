/**
 * Factory class for creating specific Command instances.
 * Contains protocol constants and helper methods for checksum generation.
 */
public final class Commands {
    private Commands(){}

    // Protocol Operation Codes
    public static final byte CMD_SET_RGB  = 1;
    public static final byte CMD_BEEP     = 2;
    public static final byte CMD_FIREMODE = 3;
    
    // Broadcast ID
    public static final byte TEAM_ALL     = (byte)0xFF;

    /**
     * Helper to compute the checksum for outgoing packets.
     */
    public static byte sum3(byte a, byte b, byte c){ return (byte)((a + b + c) & 0xFF); }

    /**
     * Creates a command to set the RGB LED color.
     * @param team Target team ID (1, 2, or TEAM_ALL)
     * @param code Color code (0=Off, 1=Green, 2=Yellow/Blue, 3=Red)
     */
    public static Command setRgb(int team, int code){
        return () -> new byte[]{ (byte)team, CMD_SET_RGB, (byte)code,
                                 sum3((byte)team, CMD_SET_RGB, (byte)code) };
    }

    /**
     * Creates a command to trigger the piezo buzzer.
     * @param team Target team ID
     * @param pattern Beep pattern ID (1=Short, 2=Long, 3=Chirp)
     */
    public static Command beep(int team, int pattern){
        return () -> new byte[]{ (byte)team, CMD_BEEP, (byte)pattern,
                                 sum3((byte)team, CMD_BEEP, (byte)pattern) };
    }

    /**
     * Creates a command to set the firing mode.
     * @param team Target team ID
     * @param mode Fire mode (0=Disable, 1=Semi, 2=Auto)
     */
    public static Command fireMode(int team, int mode){
        return () -> new byte[]{ (byte)team, CMD_FIREMODE, (byte)mode,
                                 sum3((byte)team, CMD_FIREMODE, (byte)mode) };
    }
}