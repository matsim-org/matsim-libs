package playground.johannes.eut;

/**
 * Very simple compound object without any type or <code>null</code> pointer
 * checking. Can be used as a 2-dimensional key within any <code>Map</code>.
 * 
 * @author gunnar
 * 
 */
public class Key2d<K1, K2> {

    private K1 subKey1;

    private K2 subKey2;

    public Key2d(K1 subKey1, K2 subKey2) {
        this.subKey1 = subKey1;
        this.subKey2 = subKey2;
    }

    @Override
		public boolean equals(Object o) {
        Key2d other = (Key2d) o;
        return (this.subKey1.equals(other.subKey1) && this.subKey2
                .equals(other.subKey2));
    }

    @Override
		public int hashCode() {
        /*
         * According to the List interface (for exactly two non-null elements).
         */
        return 31 * (31 + subKey1.hashCode()) + subKey2.hashCode();
    }

    public K1 getSubKey1() {
    	return subKey1;
    }
    
    public K2 getSubKey2() {
    	return subKey2;
    }

//	public int compareTo(Key2d<K1, K2> o) {
//		int result = subKey1.compareTo(o.getSubKey1());
//		return result;
//	}
}
