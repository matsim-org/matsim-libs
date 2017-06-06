// code by jph
package playground.clruch.utils;

/**
 * 
 */
public class GlobalAssert {
    public static void that(boolean status) {
        assert status;
        if (!status)
            throw new RuntimeException();
    }
}
