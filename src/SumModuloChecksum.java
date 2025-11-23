/**
 * Implementation of ChecksumStrategy using a Sum Modulo 256 algorithm.
 * This sums the byte values and truncates the result to 8 bits.
 * * Logic: (A + B + C) % 256
 */
public class SumModuloChecksum implements ChecksumStrategy {
    @Override
    public byte compute(byte a, byte b, byte c) {
        // Bitwise AND with 0xFF converts signed bytes to unsigned integers for calculation.
        // The result is cast back to byte, effectively performing a modulo 256 operation.
        int s = (a & 0xFF) + (b & 0xFF) + (c & 0xFF);
        return (byte)(s & 0xFF);
    }
}