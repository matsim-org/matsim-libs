package ch.sbb.matsim.contrib.railsim.integration;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitLine;

import java.io.File;
import java.util.function.Consumer;

public class RailsimTrainCirculationTest extends AbstractIntegrationTest {

	@Test
	void ring() {
		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "ring"));

		// Verify that the train circulates around the ring

		// THe south stop is part of the route two times and has twice as many stops
		assertThat(result)
			.allTrainsArrived()
			.trainHasStopAt("train_1", "stop_North", 5)
			.trainHasStopAt("train_1", "stop_East", 5)
			.trainHasStopAt("train_1", "stop_West", 5)
			.trainHasStopAt("train_1", "stop_South", 10);
	}

	@Test
	void shuttle() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "shuttle"));

		// There are 3 stops per direction, and the trips is done twice
		assertThat(result)
			.allTrainsHaveNumberOfStops(12)
			.allTrainsArrived();
	}

	@Test
	void shuttleMicro() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "shuttleMicro"));

		// There are 3 stops per direction, and the trips is done twice
		assertThat(result)
			.allTrainsHaveNumberOfStops(14)
			.allTrainsArrived();
	}

	@Test
	void nonContinuousSchedule() {

		Consumer<Scenario> setup = scenario -> {
			// Remove the backward line, so that trains do not return to the start via the same route
			scenario.getTransitSchedule().removeTransitLine(
				scenario.getTransitSchedule().getTransitLines().get(Id.create("shuttle_line_backward", TransitLine.class))
			);
		};

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "shuttle"), setup);
		assertThat(result)
			.allTrainsHaveNumberOfStops(6)
			.allTrainsArrived();
	}

	@Test
	void errorNonReachableStops() {

		Consumer<Scenario> setup = scenario -> {
			// Remove the backward line, so that trains do not return to the start via the same route
			scenario.getTransitSchedule().removeTransitLine(
				scenario.getTransitSchedule().getTransitLines().get(Id.create("shuttle_line_backward", TransitLine.class))
			);

			scenario.getNetwork().removeLink(Id.createLinkId("link_BM"));
			scenario.getNetwork().removeLink(Id.createLinkId("link_MA"));
		};

		// There will be no route for the circulation
		Assertions.
			assertThatCode(() -> runSimulation(new File(utils.getPackageInputDirectory(), "shuttle"), setup))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("No route found from node node_B_stop to node node_A");

	}

	@Test
	void errorNonReversibleTrains() {

		Consumer<Scenario> setup = scenario -> {
			// Set all train types to non-reversible
			scenario.getTransitVehicles().getVehicleTypes().values().forEach(t -> RailsimUtils.setTrainReversible(t, null));
		};

		Assertions
			.assertThatCode(() -> runSimulation(new File(utils.getPackageInputDirectory(), "shuttle"), setup))
			.isInstanceOf(IllegalStateException.class);
	}
}
