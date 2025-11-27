package ch.sbb.matsim.contrib.railsim.integration;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;

import ch.sbb.matsim.contrib.railsim.prepare.PrepareTurningAngleBasedRestrictions;

public class RailsimTurnRestrictionTest extends AbstractIntegrationTest {

	@Test
	void testRingWithTurnRestrictions() {
		// Run the ring scenario with turn restrictions applied
		// We expect the simulation to fail due to turn restrictions
		Assertions.assertThatThrownBy(() -> {
			runSimulation(new File(utils.getPackageInputDirectory(), "ring"),
				(Scenario scenario) -> {
					// Apply turn restrictions to the network
					Network network = scenario.getNetwork();

					// Apply restrictive turn restrictions (30-degree threshold)
					// This should prevent trains from making most turns in the ring
					PrepareTurningAngleBasedRestrictions.setTurnRestrictions(network, "rail", 30.0);

					// Verify that turn restrictions were applied
					boolean hasRestrictions = false;
					for (var link : network.getLinks().values()) {
						DisallowedNextLinks restrictions = NetworkUtils.getDisallowedNextLinks(link);
						if (restrictions != null && !restrictions.isEmpty()) {
							hasRestrictions = true;
							break;
						}
					}

					if (!hasRestrictions) {
						throw new RuntimeException("No turn restrictions were applied to the network");
					}
				});
		})
		.isInstanceOf(IllegalStateException.class)
		.hasMessageContaining("Train pt_train_1_train_type is trying to enter the disallowed link link_SouthWest from link link_South");
	}

	@Test
	void testRingWithModerateTurnRestrictions() {
		// Run the ring scenario with moderate turn restrictions
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "ring"),
			(Scenario scenario) -> {
				// Apply moderate turn restrictions (90-degree threshold)
				Network network = scenario.getNetwork();
				PrepareTurningAngleBasedRestrictions.setTurnRestrictions(network, "rail", 90.0);
			});

		// Verify that trains can still operate but with some restrictions
		assertThat(result)
			.allTrainsArrived()
			.trainHasStopAt("train_1", "stop_North", 5)
			.trainHasStopAt("train_1", "stop_East", 5)
			.trainHasStopAt("train_1", "stop_West", 5)
			.trainHasStopAt("train_1", "stop_South", 10);
	}
}
