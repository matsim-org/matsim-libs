package org.matsim.drtExperiments.run;

import org.matsim.testcases.MatsimTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


public class DummyTest {
    @RegisterExtension
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void testDrtDoorToDoor() {
        System.out.println("Starting dummy test");
        double x = 1;
        double y = 2;
        double z = x + y;
        assert x + y == z : "some thing is wrong!!!";


    }
}
