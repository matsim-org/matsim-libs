package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.vehicles.Vehicle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a car leg of a run is reduced to exactly the interval the matrix answers: from the end of
 * its first link to the start of its last one.
 * <p>
 * The events are written out by hand rather than simulated, because what is under test is precisely the
 * bookkeeping between them - which event starts the clock, which one stops it, and which links count
 * towards the distance. A leg driven on the {@link TestNetworks#corridor()} from the first link to the
 * last one has to come out as "n2 to n3, as long as the middle link took".
 */
class ExperiencedLegsTest {

	private static final Id<Vehicle> VEHICLE = Id.createVehicleId("v1");
	private static final Id<Person> PERSON = Id.createPersonId("p1");

	@Test
	void aLegIsCutAtTheNodesTheMatrixIsAskedAbout() {

		Network network = TestNetworks.corridor();
		ExperiencedLegs legs = new ExperiencedLegs(network, TransportMode.car, 100, 1);

		// n1 --[1-2]-- n2 --[2-3]-- n3 --[3-4]-- n4, driven end to end
		legs.handleEvent(new VehicleEntersTrafficEvent(100, PERSON, link(network, "1-2"), VEHICLE,
			TransportMode.car, 1));
		legs.handleEvent(new LinkLeaveEvent(110, VEHICLE, link(network, "1-2")));
		legs.handleEvent(new LinkEnterEvent(110, VEHICLE, link(network, "2-3")));
		legs.handleEvent(new LinkLeaveEvent(130, VEHICLE, link(network, "2-3")));
		legs.handleEvent(new LinkEnterEvent(130, VEHICLE, link(network, "3-4")));
		legs.handleEvent(new VehicleLeavesTrafficEvent(150, PERSON, link(network, "3-4"), VEHICLE,
			TransportMode.car, 1));

		List<ExperiencedLegs.Leg> result = legs.getLegs();
		assertEquals(1, result.size());

		ExperiencedLegs.Leg leg = result.get(0);
		assertEquals("n2", leg.fromNode().getId().toString(), "the end of the first link");
		assertEquals("n3", leg.toNode().getId().toString(), "the start of the last link");
		assertEquals(110, leg.departureTime(), 1e-9, "when the vehicle passed the first node");
		assertEquals(20, leg.travelTime(), 1e-9, "the 20 s the middle link took, and not the 50 s of the leg");
		assertEquals(TestNetworks.SPACING, leg.distance(), 1e-9, "the middle link, and neither of the others");
	}

	@Test
	void aLegThatLeftTheStudyAreaIsDropped() {

		Network network = TestNetworks.corridor();
		ExperiencedLegs legs = new ExperiencedLegs(network, TransportMode.car, 100, 1);

		legs.handleEvent(new VehicleEntersTrafficEvent(100, PERSON, link(network, "1-2"), VEHICLE,
			TransportMode.car, 1));
		legs.handleEvent(new LinkLeaveEvent(110, VEHICLE, link(network, "1-2")));
		// a link of the full network that the study area cut away: the matrix knows no such link, so it
		// would answer about a path through the area that this vehicle did not drive
		legs.handleEvent(new LinkEnterEvent(110, VEHICLE, Id.createLinkId("somewhere-else")));
		legs.handleEvent(new LinkLeaveEvent(130, VEHICLE, Id.createLinkId("somewhere-else")));
		legs.handleEvent(new LinkEnterEvent(130, VEHICLE, link(network, "3-4")));
		legs.handleEvent(new VehicleLeavesTrafficEvent(150, PERSON, link(network, "3-4"), VEHICLE,
			TransportMode.car, 1));

		assertEquals(1, legs.getNumberOfLegsSeen());
		assertEquals(1, legs.getNumberOfLegsOutsideTheStudyArea());
		assertThrows(IllegalStateException.class, legs::getLegs, "and then there is nothing to validate on");
	}

	@Test
	void aLegOfTwoLinksHasNoNodeToNodeTravelTimeAndIsDropped() {

		Network network = TestNetworks.corridor();
		ExperiencedLegs legs = new ExperiencedLegs(network, TransportMode.car, 100, 1);

		// leaving the first link and entering the last one are the same instant, so the two nodes the leg
		// would be cut at are passed at the same time
		legs.handleEvent(new VehicleEntersTrafficEvent(100, PERSON, link(network, "1-2"), VEHICLE,
			TransportMode.car, 1));
		legs.handleEvent(new LinkLeaveEvent(110, VEHICLE, link(network, "1-2")));
		legs.handleEvent(new LinkEnterEvent(110, VEHICLE, link(network, "2-3")));
		legs.handleEvent(new VehicleLeavesTrafficEvent(130, PERSON, link(network, "2-3"), VEHICLE,
			TransportMode.car, 1));

		assertEquals(1, legs.getNumberOfLegsSeen());
		assertEquals(0, legs.getNumberOfLegsOutsideTheStudyArea());
		assertThrows(IllegalStateException.class, legs::getLegs);
	}

	@Test
	void legsOfAnotherModeAreNotCollected() {

		Network network = TestNetworks.corridor();
		ExperiencedLegs legs = new ExperiencedLegs(network, TransportMode.car, 100, 1);

		legs.handleEvent(new VehicleEntersTrafficEvent(100, PERSON, link(network, "1-2"), VEHICLE,
			TransportMode.bike, 1));
		legs.handleEvent(new LinkLeaveEvent(110, VEHICLE, link(network, "1-2")));
		legs.handleEvent(new LinkEnterEvent(110, VEHICLE, link(network, "2-3")));
		legs.handleEvent(new LinkLeaveEvent(130, VEHICLE, link(network, "2-3")));
		legs.handleEvent(new LinkEnterEvent(130, VEHICLE, link(network, "3-4")));
		legs.handleEvent(new VehicleLeavesTrafficEvent(150, PERSON, link(network, "3-4"), VEHICLE,
			TransportMode.bike, 1));

		assertEquals(0, legs.getNumberOfLegsSeen());
		assertThrows(IllegalStateException.class, legs::getLegs);
	}

	@Test
	void moreLegsThanTheCapAreSampledDownToIt() {

		Network network = TestNetworks.corridor();
		ExperiencedLegs legs = new ExperiencedLegs(network, TransportMode.car, 3, 1);

		for (int i = 0; i < 50; i++) {
			Id<Vehicle> vehicle = Id.createVehicleId("v" + i);
			legs.handleEvent(new VehicleEntersTrafficEvent(i, PERSON, link(network, "1-2"), vehicle,
				TransportMode.car, 1));
			legs.handleEvent(new LinkLeaveEvent(i + 1, vehicle, link(network, "1-2")));
			legs.handleEvent(new LinkEnterEvent(i + 1, vehicle, link(network, "2-3")));
			legs.handleEvent(new LinkLeaveEvent(i + 21, vehicle, link(network, "2-3")));
			legs.handleEvent(new LinkEnterEvent(i + 21, vehicle, link(network, "3-4")));
			legs.handleEvent(new VehicleLeavesTrafficEvent(i + 30, PERSON, link(network, "3-4"), vehicle,
				TransportMode.car, 1));
		}

		assertEquals(50, legs.getNumberOfLegsSeen());
		assertEquals(3, legs.getLegs().size(), "a reservoir sample of the cap");
		assertTrue(legs.getLegs().stream().allMatch(leg -> leg.travelTime() == 20),
			"and every one of them is a real leg");
	}

	@Test
	void theLegsAreHandedToTheEvaluationAsQueries() {

		Network network = TestNetworks.corridor();
		ExperiencedLegs legs = new ExperiencedLegs(network, TransportMode.car, 100, 1);

		legs.handleEvent(new VehicleEntersTrafficEvent(100, PERSON, link(network, "1-2"), VEHICLE,
			TransportMode.car, 1));
		legs.handleEvent(new LinkLeaveEvent(110, VEHICLE, link(network, "1-2")));
		legs.handleEvent(new LinkEnterEvent(110, VEHICLE, link(network, "2-3")));
		legs.handleEvent(new LinkLeaveEvent(130, VEHICLE, link(network, "2-3")));
		legs.handleEvent(new LinkEnterEvent(130, VEHICLE, link(network, "3-4")));
		legs.handleEvent(new VehicleLeavesTrafficEvent(150, PERSON, link(network, "3-4"), VEHICLE,
			TransportMode.car, 1));

		MatrixEvaluation.Query query = legs.asQueries().get(0);
		assertEquals("n2", query.fromNode().getId().toString());
		assertEquals("n3", query.toNode().getId().toString());
		assertEquals(110, query.departureTime(), 1e-9);
		assertEquals(20, query.referenceTravelTime(), 1e-9, "the experienced travel time is the reference here");
		assertTrue(Double.isNaN(query.freeSpeedTravelTime()),
			"the free-speed travel time is one of the variants in this study, not a separate input");
	}

	private static Id<Link> link(Network network, String id) {
		return network.getLinks().get(Id.createLinkId(id)).getId();
	}
}
