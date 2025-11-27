package ch.sbb.matsim.contrib.railsim.integration;

import ch.sbb.matsim.contrib.railsim.events.RailsimFormationEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonContinuesInVehicleEvent;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class RailsimChainsAndFormationTest extends AbstractIntegrationTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void run() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "chainFormations"),
			(s) -> s.getConfig().qsim().setEndTime(30 * 3600));

		Assertions.assertThat(result.getEvents())
			.contains(new PersonContinuesInVehicleEvent(
				32580, Id.createPersonId("person1"), Id.createVehicleId("351761_1"),
				Id.createVehicleId("351761_1"), Id.create("f15", TransitStopFacility.class)))
			.contains(new PersonContinuesInVehicleEvent(
				36720, Id.createPersonId("person2"),
				Id.createVehicleId("351889_1"), Id.createVehicleId("351889_1"),
				Id.create("f74", TransitStopFacility.class)))
			.contains(new ActivityStartEvent(77060, Id.createPersonId("person1"), Id.createLinkId("15m_8x41a_pt"),
				null, "home", new Coord(679623, 5811182)))
			.contains(new ActivityStartEvent(71308, Id.createPersonId("person2"), Id.createLinkId("2f8_ip2w3_pt"),
				null, "home", new Coord(970034, 6017382)));

		Assertions.assertThat(result.getEvents())
			.contains(new RailsimFormationEvent(17820, Id.createVehicleId("351700_1_351700_2"), List.of("351700_1", "351700_2")))
			.contains(new RailsimFormationEvent(88981, Id.createVehicleId("351584_1_351543_1"), List.of("351584_1", "351543_1")));

		Assertions.assertThat(result.getEvents().stream().filter(e -> e.getEventType().equals(RailsimFormationEvent.EVENT_TYPE)))
			.hasSize(39);

		assertThat(result)
			.allTrainsArrived();
	}

	@Test
	void runSimple() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "simpleFormation"));

		assertThat(result)
			.trainHasLastArrival("train_A_1", 29442)
			.trainHasLastArrival("train_B_1", 29442)
			.trainHasLastArrival("train_A_1_train_B_1", 30262)
			.trainHasLastArrival("train_A_1b", 31329)
			.trainHasLastArrival("train_B_1b", 31339)
			.allTrainsArrived();

	}

	@Test
	void runSimpleDelayed() {

		Consumer<Scenario> f = scenario -> {

			TransitSchedule schedule = scenario.getTransitSchedule();

			TransitLine line = schedule.getTransitLines().get(Id.create("lineA_to_junction", TransitLine.class));
			TransitRoute route = line.getRoutes().get(Id.create("routeA_to_junction", TransitRoute.class));

			Departure departure = route.getDepartures().get(Id.create("dep_A_1", Departure.class));

			route.removeDeparture(departure);

			TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
			// Delay initial departure
			Departure newDeparture = factory.createDeparture(departure.getId(), departure.getDepartureTime() + 600);
			newDeparture.setVehicleId(departure.getVehicleId());
			newDeparture.setChainedDepartures(departure.getChainedDepartures());

			route.addDeparture(newDeparture);
		};

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "simpleFormation"), f);

		assertThat(result)
			.trainHasLastArrival("train_A_1", 30042)
			.trainHasLastArrival("train_B_1", 29442)
			.trainHasLastArrival("train_A_1_train_B_1", 30862)
			.trainHasLastArrival("train_A_1b", 31929)
			.trainHasLastArrival("train_B_1b", 31939)
			.allTrainsArrived();

	}
}
