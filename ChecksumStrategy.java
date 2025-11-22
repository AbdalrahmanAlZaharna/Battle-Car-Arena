public interface ChecksumStrategy {
    byte compute(byte a, byte b, byte c);
    default boolean valid(byte a, byte b, byte c, byte sum){
        return compute(a,b,c) == sum;
    }
}
	