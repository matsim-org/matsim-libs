package ch.sbb.matsim.contrib.railsim.integration;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import java.io.File;
import java.util.function.Consumer;

public class RailsimSpeedProfilesTest extends AbstractIntegrationTest {

	@Test
	void maxSpeed() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCrossLarge"));

		assertThat(result)
			.allStopDelaysAreZero()
			.trainHasLastArrival("train1", 30497)
			.trainHasLastArrival("train2", 30707);

	}

	@Test
	void adaptiveSpeed() {

		Consumer<Scenario> setup = scenario -> {
			RailsimConfigGroup config = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimConfigGroup.class);
			config.setSpeedProfile(RailsimConfigGroup.SpeedProfile.adaptive);
		};

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCrossLarge"), setup);

		assertThat(result)
			.allStopDelaysAreZero()
			.trainHasLastArrival("train1", 31052)
			.trainHasLastArrival("train2", 31262);

	}

	@Test
	void adaptiveSpeedWithDelay() {

		Consumer<Scenario> setup = scenario -> {
			RailsimConfigGroup config = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimConfigGroup.class);
			config.setSpeedProfile(RailsimConfigGroup.SpeedProfile.adaptive);

			TransitScheduleFactory f = scenario.getTransitSchedule().getFactory();
			TransitLine line = scenario.getTransitSchedule().getTransitLines().get(Id.create("line1", TransitLine.class));
			TransitRoute route = line.getRoutes().get(Id.create("line1_route1", TransitRoute.class));

			// Move departures of second train closer, the crossing train now has to wait for both to traverse the crossing
			Departure d1 = f.createDeparture(Id.create("dep1", Departure.class), Time.parseTime("8:02"));
			d1.setVehicleId(Id.createVehicleId("train5"));
			route.addDeparture(d1);

			route.removeDeparture(route.getDepartures().get(Id.create("1", Departure.class)));
		};

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCrossLarge"), setup);

		// Train 2 has to wait longer, but still arrives at the same time (1 sec before due to rounding)

		assertThat(result)
			.allStopDelaysAreZero()
			.trainHasLastArrival("train1", 31052)
			.trainHasLastArrival("train2", 31261);

	}
}
