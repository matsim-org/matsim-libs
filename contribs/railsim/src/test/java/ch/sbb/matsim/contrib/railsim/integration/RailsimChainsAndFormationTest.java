package ch.sbb.matsim.contrib.railsim.integration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonContinuesInVehicleEvent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

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

		assertThat(result)
			.allTrainsArrived();
	}

	@Test
	void runSimple() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "simpleFormation"));

		assertThat(result)
			.trainHasLastArrival("train_A_1", 29442)
			.trainHasLastArrival("train_B_1", 29442)
			.trainHasLastArrival("train_A_1_train_B_1", 30282)
			.trainHasLastArrival("train_A_1b", 31329)
			.trainHasLastArrival("train_B_1b", 31339)
			.allTrainsArrived();

	}
}
