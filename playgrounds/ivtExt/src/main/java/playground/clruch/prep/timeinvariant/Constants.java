/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.Random;

/** @author Claudio Ruch */
public enum Constants {
    ;

    // TODO Magic consts
    private static double TIME_MIN = 0.0;
    private static double TIME_MAX = 108000.0;
    private static double dayLenght = TIME_MAX - TIME_MIN;

    private static int randomSeed = 12345;
    private static Random rand = new Random(randomSeed);

    /* package */ static double getDayLength() {
        return dayLenght;
    }

    /* package */ static int nextInt(int bound) {
        return rand.nextInt(bound);
    }

    /* package */ static double nextDouble() {
        return rand.nextDouble();
    }

    /* package */ static double getMaxTime() {
        return TIME_MAX;
    }

    /* package */ static double getMinTime() {
        return TIME_MIN;
    }

}
