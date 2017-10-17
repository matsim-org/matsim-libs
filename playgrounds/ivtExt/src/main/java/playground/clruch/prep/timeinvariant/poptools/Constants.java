/**
 * 
 */
package playground.clruch.prep.timeinvariant.poptools;

import java.util.Random;

/** @author Claudio Ruch */
public enum Constants {
    ;

    // TODO Magic consts
    private static double TIME_MIN = 0.0;
    private static double TIME_MAX = 108000.0;
    private static double dayLenght = TIME_MAX - TIME_MIN;

    /* package */ static int randomSeed = 12345;
    /* package */ static Random rand = new Random(randomSeed);

    public static double getDayLength() {
        return dayLenght;
    }

    public static int nextInt(int bound) {
        return rand.nextInt(bound);
    }

    public static double getMaxTime() {
        return TIME_MAX;
    }

    public static double getMinTime() {
        return TIME_MIN;
    }

}
