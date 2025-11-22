public final class Commands {
    private Commands(){}

    public static final byte CMD_SET_RGB  = 1;
    public static final byte CMD_BEEP     = 2;
    public static final byte CMD_FIREMODE = 3;
    public static final byte TEAM_ALL     = (byte)0xFF;

    public static byte sum3(byte a, byte b, byte c){ return (byte)((a + b + c) & 0xFF); }

    public static Command setRgb(int team, int code){
        return () -> new byte[]{ (byte)team, CMD_SET_RGB, (byte)code,
                                 sum3((byte)team, CMD_SET_RGB, (byte)code) };
    }
    public static Command beep(int team, int pattern){
        return () -> new byte[]{ (byte)team, CMD_BEEP, (byte)pattern,
                                 sum3((byte)team, CMD_BEEP, (byte)pattern) };
    }
    public static Command fireMode(int team, int mode){
        return () -> new byte[]{ (byte)team, CMD_FIREMODE, (byte)mode,
                                 sum3((byte)team, CMD_FIREMODE, (byte)mode) };
    }
}
