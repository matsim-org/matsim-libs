package ch.sbb.matsim.contrib.railsim.integration;

import java.io.File;

import org.junit.jupiter.api.Test;

public class RailsimMinimumStopDurationTest extends AbstractIntegrationTest {

	@Test
	void minimumStopDuration() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "congested"));

		// First two trains depart on time, they can stop for more than 60 seconds
		assertThat(result)
			.forTrainAtStation("train1", "stop_B")
			.hasStopDuration(105)
			.hasDepartureTime("8:10");

		assertThat(result)
			.forTrainAtStation("train2", "stop_B")
			.hasStopDuration(105)
			.hasDepartureTime("8:15");

		// Late Departure
		assertThat(result)
			.forTrainAtStation("train3", "stop_C")
			.hasDepartureTime("8:40:09")
			.hasStopDuration(60);

		// All trains stop at least for 60 seconds along the route
		assertThat(result)
			.allTrainsHaveNumberOfStops(4)
			.allStopTimesExceptFirstAndLastSatisfy(t -> t.getStopDuration() >= 60);

	}
}
