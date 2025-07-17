package ch.sbb.matsim.contrib.railsim.integration;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.ConfigUtils;

import java.io.File;

public class RailsimTrainCirculationTest extends AbstractIntegrationTest {

	@Test
	void ring() {
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "ring"),
			(scenario -> ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimConfigGroup.class)
				.setVehicleCirculation(RailsimConfigGroup.VehicleCirculation.moveOnward))
		);

		// Verify that the train circulates around the triangle
		assertThat(result)
			.trainHasStopAt("train_1", "stop_North")
			.trainHasStopAt("train_1", "stop_East")
			.trainHasStopAt("train_1", "stop_South")
			.trainHasStopAt("train_1", "stop_West");
	}

	@Test
	void shuttle() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "shuttle"),
			(scenario -> ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimConfigGroup.class)
				.setVehicleCirculation(RailsimConfigGroup.VehicleCirculation.moveOnward))
		);

	}

	@Test
	void exception() {
		// TODO: Implement exception test for error conditions
	}
}
