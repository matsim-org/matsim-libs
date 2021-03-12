package org.matsim.application;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.prepare.GenerateShortDistanceTrips;
import org.matsim.application.prepare.TrajectoryToPlans;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class MATSimApplicationTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void help() {

        int ret = MATSimApplication.call(TestScenario.class, "--help");

        assertEquals("Return code should be 0", 0, ret);
    }

    @Test
    public void create() {

        Path input = Path.of(utils.getClassInputDirectory());
        Path output = Path.of(utils.getOutputDirectory());

        assertThat(input.resolve("persons.xml")).exists();

        MATSimApplication.call(TestScenario.class, "prepare", "trajectoryToPlans",
                            "--samples", "0.5", "0.1",
                            "--sample-size", "1.0",
                            "--name", "test",
                            "--population", input.resolve("persons.xml").toString(),
                            "--attributes", input.resolve("attributes.xml").toString(),
                            "--output", output.toString()
        );

        Path plans = output.resolve("test-100pct.plans.xml.gz");

        assertThat(plans).exists();
        assertThat(output.resolve("test-50pct.plans.xml.gz")).exists();

        MATSimApplication.call(TestScenario.class, "prepare", "generate-short-distance-trips",
                "--population", plans.toString(),
                "--num-trips", "2"
        );


        Path result = output.resolve("test-100pct.plans-with-trips.xml.gz");

        assertThat(result)
                .exists()
                .hasSameBinaryContentAs(input.resolve("test-100pct.plans-with-trips.xml.gz"));

    }

    @MATSimApplication.Prepare({
            TrajectoryToPlans.class,
            GenerateShortDistanceTrips.class
    })
    private static final class TestScenario extends MATSimApplication {

        // Public constructor is required to run the class
        public TestScenario() {
        }

    }

}