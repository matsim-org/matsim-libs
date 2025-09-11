package ch.sbb.matsim.contrib.railsim.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

public class RailsimChainsAndFormationTest extends AbstractIntegrationTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void run() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "chainFormations"));


	}
}
