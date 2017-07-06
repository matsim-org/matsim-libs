/**
 * 
 */
package playground.clruch.traveldata;

/**
 * @author Claudio Ruch
 *
 */
public enum TravelDataUtils {
    ;
    
    /**
     * @param dt
     * @param length
     * @return greatest common divisor for integers a and b
     */
    public static int greatestNonRestDt(int dt, int length) {
        if (length % dt == 0)
            return dt;
        return greatestNonRestDt(dt - 1, length);
    }
}
    
