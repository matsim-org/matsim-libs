// code by jph
package playground.clruch.utils;

import junit.framework.TestCase;

public class SafeConfigTest extends TestCase {
    public void testNullEx() {
        try {
            SafeConfig.wrap(null);
            assertTrue(false);
        } catch (Exception exception) {
            // ---
        }

    }
}
