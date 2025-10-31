package ch.sbb.matsim.contrib.railsim.integration;

import java.io.File;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

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

	@Test
	void rerouteSimple() {

		Consumer<Scenario> setup = scenario -> {
			for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					TransitRouteStop middleStop = route.getStops().get(1);
					middleStop.setMinimumStopDuration(80);
				}
			}
		};

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microStationRerouting"), setup);

		assertThat(result)
			.allStopTimesExceptFirstAndLastSatisfy(t -> t.getStopDuration() >= 80)
			.allTrainsArrived();

	}

	@Test
	void rerouteParallelLoopLinks() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "parallelTracksLoopLinksRerouting"));

		// Stop M1 has a minimum stop duration of 1:42

		assertThat(result)
			.trainHasNumberOfStops("train_GV_1", 2)
			.trainHasNumberOfStops("train_GV_2", 2)
			.trainHasNumberOfStops("train_GV_3", 2)
			.trainHasNumberOfStops("train_RV_1", 3)
			.trainHasNumberOfStops("train_RV_2", 3)
			.trainHasNumberOfStops("train_RV_3", 3)
			.trainHasNumberOfStops("train_RV_4", 3)
			.trainHasNumberOfStops("train_RV_5", 3)
			.trainHasNumberOfStops("train_RV_6", 3)
			.trainHasNumberOfStops("train_RV_7", 3)
			.trainHasNumberOfStops("train_RV_8", 3)
			.allStopTimesSatisfy(stop -> !stop.getFacilityId().equals("M1") || stop.getStopDuration() >= 102)
			.allTrainsArrived();

	}
}
