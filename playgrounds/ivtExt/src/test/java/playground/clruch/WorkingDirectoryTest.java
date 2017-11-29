package playground.clruch;

import java.io.File;

import junit.framework.TestCase;

public class WorkingDirectoryTest extends TestCase {
    public void testSimple() {
        File directory = new File("").getAbsoluteFile();
        System.out.println("working directory=\n" + directory);
        assertTrue(new File(directory, "pom.xml").isFile());
    }

    public void testAnother() {
        String value = "hello";
        assertEquals("hello", value);
    }
}
