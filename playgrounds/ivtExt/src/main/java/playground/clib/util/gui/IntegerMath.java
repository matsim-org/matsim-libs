// code by jph
// http://stackoverflow.com/questions/3305059/how-do-you-calculate-log-base-2-in-java-for-integers
package playground.clib.util.gui;

import java.util.Collection;

public class IntegerMath {
    private IntegerMath() {
    }

    public static int log2(int n) {
        if (n <= 0)
            throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    public static boolean isPowerOf2(int n) {
        return 0 < n ? (n & (n - 1)) == 0 : false;
    }

    /** mod that behaves like in Matlab. for instance mod(-10, 3) == 2
     * 
     * @param index
     * @param size
     * @return matlab.mod(index, size) */
    public static int mod(int index, int size) {
        int value = index % size;
        // if value is below 0, then -size < value && value < 0.
        // For instance: -3%3==0, and -2%3==-2.
        return value < 0 ? size + value : value;
    }

    /** Euclid's algorithm
     * 
     * @param a
     * @param b
     * @return greatest common divider of a and b. */
    public static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public static int lcm(int a, int b) {
        return a * (b / gcd(a, b)); // to avoid overflow
    }

    /** @param myCollection non-empty
     * @return greatest common divider of all integers in myCollection */
    public static int gcd(Collection<Integer> myCollection) {
        return myCollection.stream().reduce(IntegerMath::gcd).orElse(null);
    }

    /** @param myCollection non-empty
     * @return least common multiple of all integers in myCollection */
    public static int lcm(Collection<Integer> myCollection) {
        return myCollection.stream().reduce(IntegerMath::lcm).orElse(null);
    }

    /** integer division with intuitive handling of negative numbers
     * 
     * for instance:
     * -5/ 7 == 0, but
     * floorDiv(-5, 7) == -1
     * 
     * @param a
     * @param b is positive
     * @return */
    public static int floorDiv(int a, int b) {
        assert 0 < b;
        return 0 <= a ? a / b : (a - b + 1) / b;
    }

    /** integer division equivalent to double division followed by ceil
     * 
     * for instance:
     * 5/ 7 == 0, but
     * ceilDiv( 5, 7) == 1, since ceil(5./ 7.) == 1
     * 
     * @param a
     * @param b is positive
     * @return */
    public static int ceilDiv(int a, int b) {
        return floorDiv(a + b - 1, b);
    }

    /** method is faster than (int)Math.floor(x)
     * 
     * @param x
     * @return */
    public static int floor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    /** @param ofs
     * @param mod
     * @param min
     * @return minimal s with s ==_mod ofs && min <= s */
    public static int findMin(int ofs, int mod, int min) {
        int value = ofs - floorDiv(ofs - min, mod) * mod;
        if (value < min || min + mod <= value)
            throw new RuntimeException();
        return value;
    }

    /** @param ofs
     * @param mod
     * @param min
     * @return maximal s with s ==_mod ofs && s <= max */
    public static int findMax(int ofs, int mod, int max) {
        int value = ofs + floorDiv(max - ofs, mod) * mod;
        if (max < value || value + mod <= max)
            throw new RuntimeException();
        return value;
    }

    static void findMinMax() {
        int ofs = 3;
        int mod = 12;
        for (int count = -99; count < 100; ++count) {
            int beg = findMax(ofs, mod, count);
            int end = findMin(ofs, mod, count);
            System.out.println(String.format("max %4d <= %4d  %d]   [min %4d <= %4d  %d", //
                    beg, count, (beg - ofs) % mod, //
                    count, end, (end - ofs) % mod));
        }
    }

    public static void main(String[] args) {
        int mod = 12;
        for (int count = -15; count < 15; ++count)
            System.out.println(String.format("x=%3d  %3d  %3d", count, floorDiv(count, mod), ceilDiv(count, mod)));
        System.out.println("" + (-5 / 7));
        // findMinMax();
    }
}
