/**
 * Defines the contract for data integrity verification algorithms.
 * Implementations are responsible for calculating checksums to detect transmission errors
 * over the ZigBee wireless link.
 */
public interface ChecksumStrategy {
    /**
     * Computes a checksum byte based on three input bytes.
     * @param a First byte
     * @param b Second byte
     * @param c Third byte
     * @return The calculated checksum.
     */
    byte compute(byte a, byte b, byte c);

    /**
     * Validates if a received checksum matches the computed value for the given data bytes.
     * @return true if the computed checksum equals the provided sum.
     */
    default boolean valid(byte a, byte b, byte c, byte sum){
        return compute(a,b,c) == sum;
    }
}