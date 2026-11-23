package org.matsim.contrib.bicycle;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Feeds hand-made event sequences into {@link BicycleScoreEventsCreator} and checks the emitted
 * bicycleMotorizedInteractionScore events for each motorized interaction mode.
 */
public class BicycleScoreEventsCreatorTest {

	private static final String MOTORIZED_INTERACTION_SCORE = "bicycleMotorizedInteractionScore";
	private static final double DELTA = 1e-9;

	private static final Id<Link> L0 = Id.createLinkId("l0");
	private static final Id<Link> L1 = Id.createLinkId("l1");
	private static final Id<Link> L2 = Id.createLinkId("l2");

	private static final Id<Person> BIKE_DRIVER = Id.createPersonId("bikeDriver");
	private static final Id<Person> CAR_DRIVER = Id.createPersonId("carDriver");
	private static final Id<Vehicle> BIKE = Id.createVehicleId("bike");
	private static final Id<Vehicle> CAR = Id.createVehicleId("car");

	@Test
	void testCarCountOnBicycleLeaveLink() {
		Fixture f = new Fixture(BicycleConfigGroup.MotorizedInteraction.CAR_COUNT_ON_BICYCLE_LEAVE_LINK);
		BicycleScoreEventsCreator c = f.creator;
		String bike = f.bicycleMode;

		c.handleEvent(new VehicleEntersTrafficEvent(0., BIKE_DRIVER, L0, BIKE, bike, 1.));
		c.handleEvent(new VehicleEntersTrafficEvent(5., CAR_DRIVER, L1, CAR, TransportMode.car, 1.));
		c.handleEvent(new LinkLeaveEvent(10., BIKE, L0)); // no car on l0
		c.handleEvent(new LinkEnterEvent(10., BIKE, L1));
		c.handleEvent(new LinkLeaveEvent(20., BIKE, L1)); // one car on l1
		c.handleEvent(new LinkEnterEvent(20., BIKE, L2));
		c.handleEvent(new VehicleLeavesTrafficEvent(30., BIKE_DRIVER, L2, BIKE, bike, 1.));
		c.handleEvent(new VehicleLeavesTrafficEvent(40., CAR_DRIVER, L1, CAR, TransportMode.car, 1.));

		List<PersonScoreEvent> scores = f.motorizedInteractionScores();
		assertEquals(1, scores.size());
		assertEquals(BIKE_DRIVER, scores.get(0).getPersonId());
		assertEquals(-0.004, scores.get(0).getAmount(), DELTA);
	}

	@Test
	void testCarsPassedBicycleOnLink() {
		Fixture f = new Fixture(BicycleConfigGroup.MotorizedInteraction.CARS_PASSED_BICYCLE_ON_LINK);
		BicycleScoreEventsCreator c = f.creator;
		String bike = f.bicycleMode;

		// bike: l0 [0,1], l1 [1,100], l2 [100,110]
		// car:  l0 [5,6], l1 [6,20],  l2 [20,25]  -> overtakes the bike on l1 only
		c.handleEvent(new VehicleEntersTrafficEvent(0., BIKE_DRIVER, L0, BIKE, bike, 1.));
		c.handleEvent(new LinkLeaveEvent(1., BIKE, L0)); // must not fail although no bike was counted
		c.handleEvent(new LinkEnterEvent(1., BIKE, L1));

		c.handleEvent(new VehicleEntersTrafficEvent(5., CAR_DRIVER, L0, CAR, TransportMode.car, 1.));
		c.handleEvent(new LinkLeaveEvent(6., CAR, L0));
		c.handleEvent(new LinkEnterEvent(6., CAR, L1));
		c.handleEvent(new LinkLeaveEvent(20., CAR, L1));
		c.handleEvent(new LinkEnterEvent(20., CAR, L2));
		c.handleEvent(new VehicleLeavesTrafficEvent(25., CAR_DRIVER, L2, CAR, TransportMode.car, 1.));

		c.handleEvent(new LinkLeaveEvent(100., BIKE, L1));
		c.handleEvent(new LinkEnterEvent(100., BIKE, L2));
		c.handleEvent(new VehicleLeavesTrafficEvent(110., BIKE_DRIVER, L2, BIKE, bike, 1.));

		List<PersonScoreEvent> scores = f.motorizedInteractionScores();
		assertEquals(1, scores.size());
		assertEquals(BIKE_DRIVER, scores.get(0).getPersonId());
		assertEquals(-0.004, scores.get(0).getAmount(), DELTA);
	}

	@Test
	void testAvgCarOccupancyDuringBicycleTraversal() {
		Fixture f = new Fixture(BicycleConfigGroup.MotorizedInteraction.AVG_CAR_OCCUPANCY_DURING_BICYCLE_TRAVERSAL);
		BicycleScoreEventsCreator c = f.creator;
		String bike = f.bicycleMode;

		// bike: l0 [0,10], l1 [10,50], l2 [50,60]
		// car:  l0 [15,20], l1 [20,30], l2 [30,35]
		// -> on l1 the bike sees one car for 10 of 40 seconds: average occupancy 0.25
		c.handleEvent(new VehicleEntersTrafficEvent(0., BIKE_DRIVER, L0, BIKE, bike, 1.));
		c.handleEvent(new LinkLeaveEvent(10., BIKE, L0)); // must not fail although no bike was counted
		c.handleEvent(new LinkEnterEvent(10., BIKE, L1));

		c.handleEvent(new VehicleEntersTrafficEvent(15., CAR_DRIVER, L0, CAR, TransportMode.car, 1.));
		c.handleEvent(new LinkLeaveEvent(20., CAR, L0));
		c.handleEvent(new LinkEnterEvent(20., CAR, L1));
		c.handleEvent(new LinkLeaveEvent(30., CAR, L1)); // exposure must be updated before the car is removed
		c.handleEvent(new LinkEnterEvent(30., CAR, L2));
		c.handleEvent(new VehicleLeavesTrafficEvent(35., CAR_DRIVER, L2, CAR, TransportMode.car, 1.));

		c.handleEvent(new LinkLeaveEvent(50., BIKE, L1));
		c.handleEvent(new LinkEnterEvent(50., BIKE, L2));
		c.handleEvent(new VehicleLeavesTrafficEvent(60., BIKE_DRIVER, L2, BIKE, bike, 1.));

		List<PersonScoreEvent> scores = f.motorizedInteractionScores();
		assertEquals(1, scores.size());
		assertEquals(BIKE_DRIVER, scores.get(0).getPersonId());
		assertEquals(-0.004 * 0.25, scores.get(0).getAmount(), DELTA);
	}

	private static final class Fixture {
		final BicycleScoreEventsCreator creator;
		final String bicycleMode;
		final List<PersonScoreEvent> scoreEvents = new ArrayList<>();

		Fixture(BicycleConfigGroup.MotorizedInteraction motorizedInteraction) {
			Config config = ConfigUtils.createConfig();
			BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
			bicycleConfigGroup.setMotorizedInteractionType(motorizedInteraction);
			this.bicycleMode = bicycleConfigGroup.getBicycleMode();
			Scenario scenario = ScenarioUtils.createScenario(config);

			EventsManager eventsManager = EventsUtils.createEventsManager();
			eventsManager.addHandler(new PersonScoreEventHandler() {
				@Override
				public void handleEvent(PersonScoreEvent event) {
					scoreEvents.add(event);
				}
			});

			// the link based score is not under test here
			AdditionalBicycleLinkScore noLinkScore = (link, vehicleId, mode) -> 0.;
			this.creator = new BicycleScoreEventsCreator(scenario, eventsManager, noLinkScore);
		}

		List<PersonScoreEvent> motorizedInteractionScores() {
			return scoreEvents.stream()
				.filter(e -> MOTORIZED_INTERACTION_SCORE.equals(e.getKind()))
				.toList();
		}
	}
}
