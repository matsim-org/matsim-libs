package playground.johannes.eut;


/**
 * @author gunnar
 * 
 */
public class Arithm {

    // LOGARITHMS

    public static final double LN_2 = ln(2.0);

    public static double ln(double x) {
        return Math.log(x);
    }

    public static double ld(double x) {
        return ln(x) / LN_2;
    }

    // ...

    public static int pow(int base, int exp) {
        if (exp < 0)
            throw new IllegalArgumentException();
        int result = 1;
        for (int i = 0; i < exp; i++)
            result *= base;
        return result;
    }

    public static double si(double x) {
        return (x < 1e-6) ? 1 : Math.sin(x) / x;
    }

    public static double min(double v, double... vals) {
        double min = v;
        for (double cand : vals)
            if (cand < min)
                min = cand;
        return min;
    }

    public static int max(int v, int... vals) {
        int max = v;
        for (int cand : vals)
            if (cand > max)
                max = cand;
        return max;
    }

    public static double max(double v, double... vals) {
        double max = v;
        for (double cand : vals)
            if (cand > max)
                max = cand;
        return max;
    }

    public static double constr(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int constr(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * TODO: reimplement using Euclidean Algorithm
     */
    private static int gcd(int x, int y) {
        if (x < 1 || y < 1)
            // TODO gcd is defined for all integer numbers!
            throw new IllegalArgumentException(
                    "Arguments must be strictly positive.");
        int gcd = Math.min(x, y);
        while (x % gcd != 0 || y % gcd != 0)
            gcd--;
        return gcd;
    }

    public static int greatestCommonDivisor(int... vals) {

        if (vals == null || vals.length == 0)
            throw new IllegalArgumentException("Need at least one argument.");

        int result = vals[0];
        for (int other : vals)
            result = gcd(result, other);
        return result;
    }

    public static int round(float x) {
        return (int) Math.round(x);
    }

    public static int round(double x) {
        return (int) Math.round(x);
    }

    public static double round(double x, int digits) {
        final double ten2digits = Math.pow(10, digits);
        return round(x * ten2digits) / ten2digits;
    }

    public static double overlap(double min1, double max1, double min2,
            double max2) {

        double min = Math.max(min1, min2);
        double max = Math.min(max1, max2);

        return Math.max(0, max - min);
    }

    // ######################################################################
    // ########## MAIN FUNCTION ONLY FOR TESTING ############################
    // ######################################################################

    public static void main(String[] args) {
        System.out.println(greatestCommonDivisor(2, 4, 8, 16));
        System.out.println(greatestCommonDivisor(1, 2, 4, 6, 8, 16));
        System.out.println(greatestCommonDivisor(10, 2, 4, 6, 8, 16));
    }

}