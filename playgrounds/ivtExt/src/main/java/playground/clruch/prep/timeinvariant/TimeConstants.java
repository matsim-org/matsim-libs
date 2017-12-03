/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.Random;

import ch.ethz.idsc.tensor.Tensors;

/** @author Claudio Ruch */
public enum TimeConstants {
    ;

    // TODO Magic consts
    private static double TIME_MIN = 0.0;
    private static double TIME_MAX = 105000.0;
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

 
    /* package */ static Interval getDayInterval() {
        return new Interval(Tensors.vector(TIME_MIN), Tensors.vector(TIME_MAX));
    }

    
    /** @return random time during daylength */
    /* package */ public static double getRandomDayTime() {        
        return TimeConstants.nextDouble() * TimeConstants.getDayLength();
    }
    
    /** @return random time during daylength */
    /* package */ public static double getRandomDayTimeShift() {       
        double sign = TimeConstants.nextDouble() > 0.5? 1.0 : -1.0;
        return getRandomDayTime()*sign;
    }
}
