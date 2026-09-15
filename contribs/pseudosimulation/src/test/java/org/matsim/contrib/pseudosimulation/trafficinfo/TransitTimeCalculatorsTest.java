package org.matsim.contrib.pseudosimulation.trafficinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * The point of these two structures is that a pseudo-simulation iteration reads what the preceding
 * queue simulation measured, deterministically. These tests pin the three properties that gives:
 * a timetable fallback where nothing was measured, a measured mean where something was, and
 * retention of both across a PSim iteration.
 */
class TransitTimeCalculatorsTest {

	private static final Id<TransitLine> LINE = Id.create("L1", TransitLine.class);
	private static final Id<TransitRoute> ROUTE = Id.create("R1", TransitRoute.class);
	private static final Id<TransitStopFacility> STOP_A = Id.create("A", TransitStopFacility.class);
	private static final Id<TransitStopFacility> STOP_B = Id.create("B", TransitStopFacility.class);
	private static final Id<Vehicle> BUS = Id.createVehicleId("bus");
	private static final Id<Person> RIDER = Id.createPersonId("rider");

	private static final double BIN = 900.0;
	private static final int TOTAL_TIME = 24 * 3600;

	/** Lets a test choose the mobsim of the iteration without driving a whole controler. */
	private static final class FixedSwitcher extends MobSimSwitcher {
		private boolean qsim = true;

		FixedSwitcher(Scenario scenario) {
			super(new PSimConfigGroup(), scenario);
		}

		@Override
		public boolean isQSimIteration() {
			return qsim;
		}
	}

	/**
	 * One route, two stops 300s apart by timetable, departing hourly from 06:00.
	 */
	private static TransitSchedule schedule() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		TransitStopFacility a = factory.createTransitStopFacility(STOP_A, new org.matsim.api.core.v01.Coord(0, 0), false);
		TransitStopFacility b = factory.createTransitStopFacility(STOP_B, new org.matsim.api.core.v01.Coord(1000, 0), false);
		schedule.addStopFacility(a);
		schedule.addStopFacility(b);

		TransitRouteStop stopA = factory.createTransitRouteStopBuilder(a).departureOffset(0.0).arrivalOffset(0.0).build();
		TransitRouteStop stopB = factory.createTransitRouteStopBuilder(b).arrivalOffset(300.0).departureOffset(300.0)
				.build();

		TransitRoute route = factory.createTransitRoute(ROUTE, null, java.util.List.of(stopA, stopB), TransportMode.pt);
		for (int hour = 6; hour < 10; hour++) {
			Departure departure = factory.createDeparture(Id.create("d" + hour, Departure.class), hour * 3600.0);
			departure.setVehicleId(BUS);
			route.addDeparture(departure);
		}
		TransitLine line = factory.createTransitLine(LINE);
		line.addRoute(route);
		schedule.addTransitLine(line);
		return schedule;
	}

	private static Scenario emptyScenario() {
		return ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	@Test
	void stopStopTimeFallsBackToTheTimetableWhenNothingWasMeasured() {
		PSimStopStopTimeCalculator calculator = new PSimStopStopTimeCalculator(schedule(), BIN, TOTAL_TIME, null);
		assertEquals(300.0, calculator.getStopStopTime(STOP_A, STOP_B, 7 * 3600.0), 1e-9,
				"with no observation the timetable's own offset difference should be returned");
	}

	@Test
	void stopStopTimeReportsTheMeanOfWhatWasMeasured() {
		PSimStopStopTimeCalculator calculator = new PSimStopStopTimeCalculator(schedule(), BIN, TOTAL_TIME, null);
		// Two runs through the same bin, 400s and 500s: the mean is 450s, not the last value.
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0, BUS, STOP_A, 0.0));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0 + 400, BUS, STOP_B, 0.0));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0 + 100, BUS, STOP_A, 0.0));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0 + 600, BUS, STOP_B, 0.0));

		assertEquals(450.0, calculator.getStopStopTime(STOP_A, STOP_B, 7 * 3600.0), 1e-9,
				"the measured value should be the mean over the bin");
	}

	@Test
	void stopStopTimeIsStableAcrossRepeatedLookups() {
		PSimStopStopTimeCalculator calculator = new PSimStopStopTimeCalculator(schedule(), BIN, TOTAL_TIME, null);
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0, BUS, STOP_A, 0.0));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0 + 400, BUS, STOP_B, 0.0));

		double first = calculator.getStopStopTime(STOP_A, STOP_B, 7 * 3600.0);
		for (int i = 0; i < 50; i++) {
			assertEquals(first, calculator.getStopStopTime(STOP_A, STOP_B, 7 * 3600.0), 0.0,
					"repeated lookups must agree; a sampled measure is what this replaces");
		}
	}

	@Test
	void stopStopTimeSurvivesAPSimIterationAndIsRemeasuredOnAQSimIteration() {
		FixedSwitcher switcher = new FixedSwitcher(emptyScenario());
		PSimStopStopTimeCalculator calculator = new PSimStopStopTimeCalculator(schedule(), BIN, TOTAL_TIME, switcher);
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0, BUS, STOP_A, 0.0));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0 + 400, BUS, STOP_B, 0.0));
		assertEquals(400.0, calculator.getStopStopTime(STOP_A, STOP_B, 7 * 3600.0), 1e-9);

		switcher.qsim = false;
		calculator.reset(1);
		assertEquals(400.0, calculator.getStopStopTime(STOP_A, STOP_B, 7 * 3600.0), 1e-9,
				"a PSim iteration must keep what the preceding QSim iteration measured");

		switcher.qsim = true;
		calculator.reset(2);
		assertEquals(300.0, calculator.getStopStopTime(STOP_A, STOP_B, 7 * 3600.0), 1e-9,
				"a QSim iteration measures afresh, so the timetable fallback returns");
	}

	@Test
	void stopStopTimeIgnoresEventsEmittedDuringAPSimIteration() {
		FixedSwitcher switcher = new FixedSwitcher(emptyScenario());
		PSimStopStopTimeCalculator calculator = new PSimStopStopTimeCalculator(schedule(), BIN, TOTAL_TIME, switcher);
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0, BUS, STOP_A, 0.0));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0 + 400, BUS, STOP_B, 0.0));

		switcher.qsim = false;
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0, BUS, STOP_A, 0.0));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(7 * 3600.0 + 9999, BUS, STOP_B, 0.0));

		assertEquals(400.0, calculator.getStopStopTime(STOP_A, STOP_B, 7 * 3600.0), 1e-9,
				"PSim derives its own transit times from this structure; feeding them back closes a loop");
	}

	@Test
	void waitTimeFallsBackToTheScheduledHeadwayWhenNothingWasMeasured() {
		PSimWaitTimeCalculator calculator = new PSimWaitTimeCalculator(schedule(), BIN, TOTAL_TIME, null);
		// Departures are hourly on the hour; a passenger at the end of the 06:00-06:15 bin waits
		// until 07:00, i.e. 45 minutes.
		double wait = calculator.getRouteStopWaitTime(LINE, ROUTE, STOP_A, 6 * 3600.0);
		assertEquals(2700.0, wait, 1e-9, "the timetable implies a 45 minute wait at the end of that bin");
	}

	@Test
	void waitTimeReportsWhatWasMeasured() {
		PSimWaitTimeCalculator calculator = new PSimWaitTimeCalculator(schedule(), BIN, TOTAL_TIME, null);
		calculator.handleEvent(new TransitDriverStartsEvent(6 * 3600.0, Id.createPersonId("driver"), BUS, LINE, ROUTE,
				Id.create("d6", Departure.class)));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(6 * 3600.0 + 100, BUS, STOP_A, 0.0));
		calculator.handleEvent(new PersonDepartureEvent(6 * 3600.0, RIDER, null, TransportMode.pt, TransportMode.pt));
		calculator.handleEvent(new PersonEntersVehicleEvent(6 * 3600.0 + 120, RIDER, BUS));

		assertEquals(120.0, calculator.getRouteStopWaitTime(LINE, ROUTE, STOP_A, 6 * 3600.0), 1e-9,
				"a measured wait should replace the scheduled one");
	}

	@Test
	void waitTimeSurvivesAPSimIteration() {
		FixedSwitcher switcher = new FixedSwitcher(emptyScenario());
		PSimWaitTimeCalculator calculator = new PSimWaitTimeCalculator(schedule(), BIN, TOTAL_TIME, switcher);
		calculator.handleEvent(new TransitDriverStartsEvent(6 * 3600.0, Id.createPersonId("driver"), BUS, LINE, ROUTE,
				Id.create("d6", Departure.class)));
		calculator.handleEvent(new VehicleArrivesAtFacilityEvent(6 * 3600.0 + 100, BUS, STOP_A, 0.0));
		calculator.handleEvent(new PersonDepartureEvent(6 * 3600.0, RIDER, null, TransportMode.pt, TransportMode.pt));
		calculator.handleEvent(new PersonEntersVehicleEvent(6 * 3600.0 + 120, RIDER, BUS));

		switcher.qsim = false;
		calculator.reset(1);
		assertEquals(120.0, calculator.getRouteStopWaitTime(LINE, ROUTE, STOP_A, 6 * 3600.0), 1e-9,
				"a PSim iteration must keep what the preceding QSim iteration measured");

		switcher.qsim = true;
		calculator.reset(2);
		assertTrue(calculator.getRouteStopWaitTime(LINE, ROUTE, STOP_A, 6 * 3600.0) > 120.0,
				"a QSim iteration measures afresh, so the scheduled wait returns");
	}
}
