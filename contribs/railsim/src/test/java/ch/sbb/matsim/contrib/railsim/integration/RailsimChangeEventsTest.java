package ch.sbb.matsim.contrib.railsim.integration;

import org.junit.jupiter.api.Test;

import java.io.File;

public class RailsimChangeEventsTest extends AbstractIntegrationTest {


	@Test
	void trackClosure() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCrossLarge"),
			useEvents("trackClosure"), null);


		// TODO
	}

	@Test
	void speedAdjustment() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCrossLarge"),
			useEvents("speedAdjustment"), null);


		// TODO

	}
}
