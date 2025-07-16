package ch.sbb.matsim.contrib.railsim.integration;

import org.junit.jupiter.api.Test;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;

import java.io.File;
import java.util.List;

public class RailsimTrainCirculationTest extends AbstractIntegrationTest {

	@Test
	void ring() {
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "ring"));

		// Verify that the train circulates around the triangle
		assertThat(result)
			.trainHasStopAt("train_1", "stop_A")
			.trainHasStopAt("train_1", "stop_B")
			.trainHasStopAt("train_1", "stop_C");
	}

	@Test
	void shuttle() {
		// TODO: Implement shuttle test with back-and-forth movement
	}

	@Test
	void exception() {
		// TODO: Implement exception test for error conditions
	}
}
