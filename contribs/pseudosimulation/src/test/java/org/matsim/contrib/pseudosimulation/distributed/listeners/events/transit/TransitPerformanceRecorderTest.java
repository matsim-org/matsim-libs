package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * The recorder turns a QSim transit event stream into the dwell-event history that PSim
 * iterations read back. Without it, PSim gives every transit leg no travel time at all.
 */
class TransitPerformanceRecorderTest {

	private static final Id<Vehicle> VEHICLE = Id.createVehicleId("bus");
	private static final Id<Person> DRIVER = Id.create("driver", Person.class);
	private static final Id<Person> RIDER = Id.create("rider", Person.class);
	private static final Id<TransitLine> LINE = Id.create("line", TransitLine.class);
	private static final Id<TransitRoute> ROUTE = Id.create("route", TransitRoute.class);
	private static final Id<Departure> DEPARTURE = Id.create("departure", Departure.class);
	private static final Id<TransitStopFacility> FIRST_STOP = Id.create("first", TransitStopFacility.class);
	private static final Id<TransitStopFacility> SECOND_STOP = Id.create("second", TransitStopFacility.class);

	private EventsManager events;
	private TransitPerformanceRecorder recorder;

	@BeforeEach
	void setUp() {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);

		VehicleType type = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class));
		type.getCapacity().setSeats(8).setStandingRoom(2);
		scenario.getTransitVehicles().addVehicleType(type);
		scenario.getTransitVehicles().addVehicle(VehicleUtils.createVehicle(VEHICLE, type));

		events = EventsUtils.createEventsManager();
		recorder = new TransitPerformanceRecorder(scenario, events, null);
		recorder.getTransitPerformance().setBoardingModel(new BoardingModelIgnoringOccupancy());
	}

	private void driveOneRun() {
		events.processEvent(new TransitDriverStartsEvent(0.0, DRIVER, VEHICLE, LINE, ROUTE, DEPARTURE));
		events.processEvent(new VehicleArrivesAtFacilityEvent(100.0, VEHICLE, FIRST_STOP, 0.0));
		events.processEvent(new PersonEntersVehicleEvent(110.0, RIDER, VEHICLE));
		events.processEvent(new VehicleDepartsAtFacilityEvent(120.0, VEHICLE, FIRST_STOP, 0.0));
		events.processEvent(new VehicleArrivesAtFacilityEvent(400.0, VEHICLE, SECOND_STOP, 0.0));
		events.processEvent(new PersonLeavesVehicleEvent(410.0, RIDER, VEHICLE));
		events.processEvent(new VehicleDepartsAtFacilityEvent(420.0, VEHICLE, SECOND_STOP, 0.0));
	}

	@Test
	void recordsWaitingAndInVehicleTimeBetweenStops() {
		driveOneRun();

		Tuple<Double, Double> travelTime = recorder.getTransitPerformance()
				.getRouteTravelTime(LINE, ROUTE, FIRST_STOP, SECOND_STOP, 40.0);

		assertEquals(60.0, travelTime.getFirst(), "waiting time until the vehicle arrives");
		assertEquals(300.0, travelTime.getSecond(), "in-vehicle time between the two stops");
	}

	@Test
	void reportsInfinityWhenNothingWasRecordedForTheRequestedRoute() {
		driveOneRun();

		Tuple<Double, Double> unknownLine = recorder.getTransitPerformance().getRouteTravelTime(
				Id.create("other", TransitLine.class), ROUTE, FIRST_STOP, SECOND_STOP, 40.0);

		assertEquals(Double.POSITIVE_INFINITY, unknownLine.getFirst());
		assertEquals(Double.POSITIVE_INFINITY, unknownLine.getSecond());
	}

	@Test
	void reportsInfinityForDeparturesAfterTheLastRecordedArrival() {
		driveOneRun();

		Tuple<Double, Double> tooLate = recorder.getTransitPerformance()
				.getRouteTravelTime(LINE, ROUTE, FIRST_STOP, SECOND_STOP, 10_000.0);

		assertEquals(Double.POSITIVE_INFINITY, tooLate.getFirst());
	}

	@Test
	void keepsTheRecordedHistoryWhenTheNextIterationIsAPSimIteration() {
		driveOneRun();
		TransitPerformance recorded = recorder.getTransitPerformance();

		// A null switcher stands for "no PSim in play"; the recorder then always starts afresh.
		events.resetHandlers(1);

		assertNotSame(recorded, recorder.getTransitPerformance(),
				"a QSim iteration must start from an empty history");
		assertSame(recorder.getTransitPerformance(), recorder.getTransitPerformance());
	}
}
