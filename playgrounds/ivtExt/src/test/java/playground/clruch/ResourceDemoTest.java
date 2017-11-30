// code by jph
package playground.clruch;

import java.io.File;
import java.util.Objects;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.ArrayQ;
import ch.ethz.idsc.tensor.io.ResourceData;
import junit.framework.TestCase;

/** THIS FILE IS FOR DEMONSTRATION PURPOSE
 * 
 * DO NOT MODIFY THIS FILE
 * INSTEAD MAKE A COPY AND THEN MODIFY */
public class ResourceDemoTest extends TestCase {
    /** upon execution of test the working directory is of the form
     * /home/datahaki/Projects/matsim/playgrounds/ivtExt
     * 
     * while we run the test locally (and as a jar file not on the server)
     * we can access content of files directory using File */
    public void testSimple() {
        // ResourceData.of("src/test/resources/test/scenarios/siouxfalls-2014-reduced/config_default.xml");
        File file = new File("src/test/resources/test/scenarios/siouxfalls-2014-reduced/config_default.xml");
        assertTrue(file.exists());
        assertTrue(2000 < file.length());
    }

    /** another pattern uses
     * ResourceData.class.getResourceAsStream
     * 
     * this is required when resources don't exist as files but are packed in jar files
     * and can only be accessed as an input stream */
    public void testResources() {
        /** the location of the resource is relative to the folder "src/test/resources/"
         * but access to resource in "src/main/resources" is also possible */
        Tensor csv = ResourceData.of("/demo/simple.csv");
        assertTrue(Objects.nonNull(csv));
        assertEquals(csv.length(), 3);
        assertFalse(ArrayQ.of(csv));
    }
}
