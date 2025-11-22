public class SumModuloChecksum implements ChecksumStrategy {
    @Override
    public byte compute(byte a, byte b, byte c) {
        int s = (a & 0xFF) + (b & 0xFF) + (c & 0xFF);
        return (byte)(s & 0xFF);
    }
}
