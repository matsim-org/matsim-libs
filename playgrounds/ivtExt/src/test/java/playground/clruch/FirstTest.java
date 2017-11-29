package playground.clruch;

import junit.framework.TestCase;

public class FirstTest extends TestCase {
    public void testSimple() {
        assertTrue(true);
    }

    public void testAnother() {
        String value = "hello";
        assertEquals("hello", value);
    }
}
