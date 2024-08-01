package org.matsim.contrib.discrete_mode_choice.components.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contribs.discrete_mode_choice.components.utils.ScheduleWaitingTimeEstimator;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ScheduleWaitingTimeEstimatorTest {
	@Test
	void testValidSingleCase() throws IOException {
		TransitSchedule schedule = createSchedule();

		ScheduleWaitingTimeEstimator estimator = new ScheduleWaitingTimeEstimator(schedule);

		List<? extends PlanElement> elements;
		double waitingTime;

		// Test 1
		elements = createElements(schedule, Arrays.asList( //
				new Trip(2025.0, "f3") // 5 seconds before 2000 bus departs there
		));

		waitingTime = estimator.estimateWaitingTime(elements);
		assertEquals(5.0, waitingTime, 1e-6);
		
		// Test 1
		elements = createElements(schedule, Arrays.asList( //
				new Trip(2035.0, "f3") // 5 seconds after 2000 bus departs there, take 3000!
		));

		waitingTime = estimator.estimateWaitingTime(elements);
		assertEquals(995.0, waitingTime, 1e-6);
	}

	@Test
	void testValidMultiCase() throws IOException {
		TransitSchedule schedule = createSchedule();

		ScheduleWaitingTimeEstimator estimator = new ScheduleWaitingTimeEstimator(schedule);

		List<? extends PlanElement> elements;
		double waitingTime;

		// Test 1
		elements = createElements(schedule, Arrays.asList( //
				new Trip(2030.0 - 20.0, "f3"), // 20 seconds before 2000 bus departs there
				new Trip(2035.0, "f3") // 5 seconds after 2000 bus departs there, take 3000!
		));

		waitingTime = estimator.estimateWaitingTime(elements);
		assertEquals(20.0 + 995.0, waitingTime, 1e-6);
	}


	@Test
	void testInvalidCase() throws IOException {
		TransitSchedule schedule = createSchedule();

		ScheduleWaitingTimeEstimator estimator = new ScheduleWaitingTimeEstimator(schedule);

		List<? extends PlanElement> elements;
		double waitingTime;

		// Test 1
		elements = createElements(schedule, Arrays.asList( //
				new Trip(9000.0, "f3") // No departure after this time!
		));

		waitingTime = estimator.estimateWaitingTime(elements);
		assertEquals(0.0, waitingTime, 1e-6);
	}

	// Stuff to set up the test starts here. Not relevant for the actual code.

	private TransitSchedule createSchedule() {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();

		TransitSchedule schedule = factory.createTransitSchedule();

		TransitStopFacility facility1 = factory.createTransitStopFacility(Id.create("f1", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);
		TransitStopFacility facility2 = factory.createTransitStopFacility(Id.create("f2", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);
		TransitStopFacility facility3 = factory.createTransitStopFacility(Id.create("f3", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);
		TransitStopFacility facility4 = factory.createTransitStopFacility(Id.create("f4", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);
		TransitStopFacility facility5 = factory.createTransitStopFacility(Id.create("f5", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);

		schedule.addStopFacility(facility1);
		schedule.addStopFacility(facility2);
		schedule.addStopFacility(facility3);
		schedule.addStopFacility(facility4);
		schedule.addStopFacility(facility5);

		TransitLine transitLine = factory.createTransitLine(Id.create("myLine", TransitLine.class));
		schedule.addTransitLine(transitLine);

		List<TransitRouteStop> stops = new LinkedList<>();

		stops.add(factory.createTransitRouteStop(facility1, 10.0 - 1.1, 10.0));
		stops.add(factory.createTransitRouteStop(facility2, 20.0 - 1.1, 20.0));
		stops.add(factory.createTransitRouteStop(facility3, 30.0 - 1.1, 30.0));
		stops.add(factory.createTransitRouteStop(facility4, 40.0 - 1.1, 40.0));
		stops.add(factory.createTransitRouteStop(facility5, 50.0 - 1.1, 50.0));

		TransitRoute transitRoute = factory.createTransitRoute(Id.create("myRoute", TransitRoute.class), null, stops,
				"bus");
		transitLine.addRoute(transitRoute);

		Departure departure;

		departure = factory.createDeparture(Id.create("departure1", Departure.class), 1000.0);
		transitRoute.addDeparture(departure);

		departure = factory.createDeparture(Id.create("departure2", Departure.class), 2000.0);
		transitRoute.addDeparture(departure);

		departure = factory.createDeparture(Id.create("departure3", Departure.class), 3000.0);
		transitRoute.addDeparture(departure);

		departure = factory.createDeparture(Id.create("departure4", Departure.class), 4000.0);
		transitRoute.addDeparture(departure);

		departure = factory.createDeparture(Id.create("departure5", Departure.class), 5000.0);
		transitRoute.addDeparture(departure);

		return schedule;
	}

	private class Trip {
		double departureTime;
		String facility;

		Trip(double departureTime, String facility) {
			this.departureTime = departureTime;
			this.facility = facility;
		}
	}

	private List<? extends PlanElement> createElements(TransitSchedule schedule, List<Trip> trips) {
		PopulationFactory factory = PopulationUtils.getFactory();

		TransitLine transitLine = schedule.getTransitLines().get(Id.create("myLine", TransitLine.class));
		TransitRoute transitRoute = transitLine.getRoutes().get(Id.create("myRoute", TransitRoute.class));

		List<PlanElement> elements = new LinkedList<>();

		Leg leg;
		Activity activity;

		if (trips.size() == 0) {
			leg = factory.createLeg("non_network_walk");
			elements.add(leg);
		} else {
			leg = factory.createLeg("non_network_walk");
			elements.add(leg);

			activity = factory.createActivityFromCoord("pt interaction", new Coord(0.0, 0.0));
			activity.setMaximumDuration(0.0);
			elements.add(activity);

			for (Trip trip : trips) {
				leg = factory.createLeg("pt");
				leg.setDepartureTime(trip.departureTime);

				TransitStopFacility facility = schedule.getFacilities()
						.get(Id.create(trip.facility, TransitStopFacility.class));

				TransitPassengerRoute route = new DefaultTransitPassengerRoute(facility, transitLine, transitRoute,
						facility);
				leg.setRoute(route);

				elements.add(leg);

				activity = factory.createActivityFromCoord("pt interaction", new Coord(0.0, 0.0));
				activity.setMaximumDuration(0.0);
				elements.add(activity);
			}

			leg = factory.createLeg("non_network_walk");
			elements.add(leg);
		}

		return elements;
	}
}
