package org.matsim.application;

import org.junit.Test;
import org.matsim.application.prepare.CreateNetworkFromSumo;
import org.matsim.application.prepare.TrajectoryToPlans;
import org.matsim.application.prepare.RemoveRoutesFromPlans;
import org.matsim.application.prepare.GenerateShortDistanceTrips;

import static org.junit.Assert.assertEquals;

public class MATSimApplicationTest {

    @Test
    public void help() {

        int ret = MATSimApplication.call(TestScenario.class, "--help");

        assertEquals("Return code should be 0", 0, ret);
    }

    @Test
    public void pipeline() {

        MATSimApplication.call(TestScenario.class, "prepare", "trajectoryToPlans");
        // TODO: generate some test data

    }

    @MATSimApplication.Prepare({
            TrajectoryToPlans.class,
            CreateNetworkFromSumo.class,
            RemoveRoutesFromPlans.class,
            GenerateShortDistanceTrips.class
    })
    private static final class TestScenario extends MATSimApplication {

        // Public constructor is required to run the class
        public TestScenario() {
        }

    }

}