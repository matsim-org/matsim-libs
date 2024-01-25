package org.matsim.core.mobsim.hermes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.hermes.HermesTest.LinkEnterEventCollector;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitRouteStopImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Various unit tests checking various pt-related things in HERMES.
 *
 * Most of the tests here cover issues described in <a href="https://github.com/matsim-org/matsim-libs/issues/1248">#1248</a>.
 *
 * @author mrieser / Simunto
 */
public class HermesTransitTest {

	protected static Hermes createHermes(MutableScenario scenario, EventsManager events) {
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		return new HermesBuilder().build(scenario, events);
	}

	protected static Hermes createHermes(Fixture f, EventsManager events) {
		return createHermes(f.scenario, events);
	}

	protected static Hermes createHermes(Scenario scenario, EventsManager events) {
		return createHermes(scenario, events, true);
	}

	protected static Hermes createHermes(Scenario scenario, EventsManager events, boolean prepareForSim) {
		if (prepareForSim) {
			PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		}
		return new HermesBuilder().build(scenario, events);
	}

	@BeforeEach
	public void prepareTest() {
		Id.resetCaches();
		ScenarioImporter.flush();
		HermesConfigGroup.SIM_STEPS = 30 * 60 * 60;
	}

	/**
	 * Makes sure Hermes works also when not all stop facilities are used by transit lines.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testSuperflousStopFacilities(boolean isDeterministic) {
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop0 = sf.createTransitStopFacility(Id.create(0, TransitStopFacility.class), new Coord(0, 0), false); // create an unused Id<TransitStopFacility> first
		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2000, 20), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop0.setLinkId(f.link1.getId());
		stop1.setLinkId(f.link1.getId());
		stop2.setLinkId(f.link2.getId());
		stop3.setLinkId(f.link3.getId());

		schedule.addStopFacility(stop0);
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId()), f.link3.getId());
		List<TransitRouteStop> stops = List.of(
				new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
				new TransitRouteStopImpl.Builder().stop(stop2).departureOffset(300).build(),
				new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
		);
		TransitRoute route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
		Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 7*3600);
		dep1.setVehicleId(ptVeh1.getId());
		route1.addDeparture(dep1);
		line1.addRoute(route1);

		schedule.addTransitLine(line1);

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(6*3600);
		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		leg.setRoute(new DefaultTransitPassengerRoute(f.link1.getId(), f.link3.getId(), stop1.getId(), stop2.getId(), line1.getId(), route1.getId()));
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);
		EventsCollector allEventsCollector = new EventsCollector();
		events.addHandler(allEventsCollector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */

		for (Event event : allEventsCollector.getEvents()) {
			System.out.println(event.toString());
		}

		// Not having an Exception here is already good :-)
		Assertions.assertEquals(2, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(f.link2.getId(), collector.events.get(0).getLinkId(), "wrong link in first event.");
		Assertions.assertEquals(f.link3.getId(), collector.events.get(1).getLinkId(), "wrong link in second event.");
	}

	/**
	 * Makes sure Hermes works also when transit routes in different lines have the same Id
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testRepeatedRouteIds(boolean isDeterministic) {
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop0 = sf.createTransitStopFacility(Id.create(0, TransitStopFacility.class), new Coord(0, 0), false); // create an unused Id<TransitStopFacility> first
		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2000, 20), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop0.setLinkId(f.link1.getId());
		stop1.setLinkId(f.link1.getId());
		stop2.setLinkId(f.link2.getId());
		stop3.setLinkId(f.link3.getId());

		schedule.addStopFacility(stop0);
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1;
		TransitRoute route1;
		{
			line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId()), f.link3.getId());
			List<TransitRouteStop> stops = List.of(
					new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
					new TransitRouteStopImpl.Builder().stop(stop2).departureOffset(300).build(),
					new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
			);
			route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
			Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 7 * 3600);
			dep1.setVehicleId(ptVeh1.getId());
			route1.addDeparture(dep1);
			line1.addRoute(route1);

			schedule.addTransitLine(line1);
		}

		TransitLine line2;
		TransitRoute route2;
		{
			line2 = sf.createTransitLine(Id.create(2, TransitLine.class));
			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId()), f.link3.getId());
			// the second route should be shorter. when the first will be used, it originally triggered an ArrayOutOfBounds exception as this route only has 2 stops, vs. in line 1 it has 3 stops
			List<TransitRouteStop> stops = List.of(
					new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
					new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
			);
			route2 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
			Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 6 * 3600);
			dep1.setVehicleId(ptVeh1.getId());
			route2.addDeparture(dep1);
			line2.addRoute(route2);

			schedule.addTransitLine(line2);
		}

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(6*3600);
		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		leg.setRoute(new DefaultTransitPassengerRoute(f.link1.getId(), f.link3.getId(), stop1.getId(), stop2.getId(), line1.getId(), route1.getId()));
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */
		// Not having an Exception here is already good :-)
		Assertions.assertEquals(4, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(f.link2.getId(), collector.events.get(0).getLinkId(), "wrong link in first event.");
		Assertions.assertEquals(f.link3.getId(), collector.events.get(1).getLinkId(), "wrong link in second event.");
	}

	/**
	 * Makes sure Hermes works with TransitRoutes where two successive stops are on the same link.
	 * Originally, this resulted in wrong events because there was an exception that there is at most one stop per link.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testConsecutiveStopsWithSameLink_1(boolean isDeterministic) {
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2900, 29), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop1.setLinkId(f.link1.getId());
		stop2.setLinkId(f.link3.getId());
		stop3.setLinkId(f.link3.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId()), f.link3.getId());
		List<TransitRouteStop> stops = List.of(
				new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
				new TransitRouteStopImpl.Builder().stop(stop2).departureOffset(300).build(),
				new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
		);
		TransitRoute route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
		Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 7*3600);
		dep1.setVehicleId(ptVeh1.getId());
		route1.addDeparture(dep1);
		line1.addRoute(route1);

		schedule.addTransitLine(line1);

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(6*3600);
		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		leg.setRoute(new DefaultTransitPassengerRoute(f.link1.getId(), f.link3.getId(), stop1.getId(), stop3.getId(), line1.getId(), route1.getId()));
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);
		EventsCollector allEventsCollector = new EventsCollector();
		events.addHandler(allEventsCollector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */

		List<VehicleArrivesAtFacilityEvent> stopArrivalEvents = new ArrayList<>();
		for (Event event : allEventsCollector.getEvents()) {
			System.out.println(event.toString());
			if (event instanceof VehicleArrivesAtFacilityEvent) {
				stopArrivalEvents.add((VehicleArrivesAtFacilityEvent) event);
			}
		}

		Assertions.assertEquals(2, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(f.link2.getId(), collector.events.get(0).getLinkId(), "wrong link in first event.");
		Assertions.assertEquals(f.link3.getId(), collector.events.get(1).getLinkId(), "wrong link in second event.");

		Assertions.assertEquals(3, stopArrivalEvents.size(), "wrong number of stops served.");
		Assertions.assertEquals(stop1.getId(), stopArrivalEvents.get(0).getFacilityId(), "wrong stop id at first stop.");
		Assertions.assertEquals(stop2.getId(), stopArrivalEvents.get(1).getFacilityId(), "wrong stop id at 2nd stop.");
		Assertions.assertEquals(stop3.getId(), stopArrivalEvents.get(2).getFacilityId(), "wrong stop id at 3rd stop.");

	}

	/**
	 * Makes sure Hermes works with TransitRoutes where two successive stops are on the same link.
	 * Originally, this resulted in problems as the distance between the stops was 0 meters, given they are on the same link,
	 * and this 0 meters than resulted in infinite values somewhere later on.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testConsecutiveStopsWithSameLink_2(boolean isDeterministic) {
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2900, 29), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop1.setLinkId(f.link1.getId());
		stop2.setLinkId(f.link3.getId());
		stop3.setLinkId(f.link3.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId(), f.link3.getId(), f.linkX.getId(), f.link1.getId(), f.link2.getId()), f.link3.getId()); // the vehicle must drive a loop to trigger the original error.
		List<TransitRouteStop> stops = List.of(
				new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
				new TransitRouteStopImpl.Builder().stop(stop2).departureOffset(300).build(),
				new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
		);
		TransitRoute route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
		Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 7*3600);
		dep1.setVehicleId(ptVeh1.getId());
		route1.addDeparture(dep1);
		line1.addRoute(route1);

		schedule.addTransitLine(line1);

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(6*3600);
		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		leg.setRoute(new DefaultTransitPassengerRoute(f.link1.getId(), f.link3.getId(), stop1.getId(), stop2.getId(), line1.getId(), route1.getId()));
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);
		EventsCollector allEventsCollector = new EventsCollector();
		events.addHandler(allEventsCollector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */

		for (Event event : allEventsCollector.getEvents()) {
			System.out.println(event.toString());
		}

		// Not having an Exception here is already good :-)
//		collector.events.sort((o1, o2) -> Double.compare(o1.getTime(), o2.getTime()));
//
//		System.out.println("-----");
//		for (Event event : collector.events) {
//			System.out.println(event.toString());
//		}

		Assertions.assertEquals(6, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(f.link2.getId(), collector.events.get(0).getLinkId(), "wrong link in first event.");
		Assertions.assertEquals(f.link3.getId(), collector.events.get(1).getLinkId(), "wrong link in second event.");
//		Assert.assertEquals("wrong link in second event.", f.linkX.getId(), collector.events.get(2).getLinkId());
//		Assert.assertEquals("wrong link in second event.", f.link1.getId(), collector.events.get(3).getLinkId());
//		Assert.assertEquals("wrong link in second event.", f.link2.getId(), collector.events.get(4).getLinkId());
//		Assert.assertEquals("wrong link in second event.", f.link3.getId(), collector.events.get(5).getLinkId());
	}

	/**
	 * Makes sure Hermes does not produce exceptions when the configured end time is before the latest transit event
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testEarlyEnd(boolean isDeterministic) {
		double baseTime = 30 * 3600 - 10; // HERMES has a default of 30:00:00 as end time, so let's start later
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2900, 29), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop1.setLinkId(f.link1.getId());
		stop2.setLinkId(f.link2.getId());
		stop3.setLinkId(f.link3.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId()), f.link3.getId());
		List<TransitRouteStop> stops = List.of(
				new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
				new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
		);
		TransitRoute route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
		Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), baseTime);
		dep1.setVehicleId(ptVeh1.getId());
		route1.addDeparture(dep1);
		line1.addRoute(route1);

		schedule.addTransitLine(line1);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector allEventsCollector = new EventsCollector();
		events.addHandler(allEventsCollector);
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */

		for (Event event : allEventsCollector.getEvents()) {
			System.out.println(event.toString());
		}

		// Not having an Exception here is already good :-)
		Assertions.assertEquals(1, collector.events.size(), "wrong number of link enter events.");
		Assertions.assertEquals(f.link2.getId(), collector.events.get(0).getLinkId(), "wrong link in first event.");
		// there should be no more events after that, as at 30:00:00 the simulation should stop
	}

	/**
	 * Makes sure Hermes correctly handles strange transit routes with some links before the first stop is served.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testLinksAtRouteStart(boolean isDeterministic) {
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2900, 29), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop1.setLinkId(f.link3.getId());
		stop2.setLinkId(f.link3.getId());
		stop3.setLinkId(f.linkX.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId(), f.link3.getId()), f.linkX.getId());
		List<TransitRouteStop> stops = List.of(
				new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
				new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
		);
		TransitRoute route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
		Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 7*3600);
		dep1.setVehicleId(ptVeh1.getId());
		route1.addDeparture(dep1);
		line1.addRoute(route1);

		schedule.addTransitLine(line1);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector allEventsCollector = new EventsCollector();
		events.addHandler(allEventsCollector);
		LinkEnterEventCollector collector = new LinkEnterEventCollector();
		events.addHandler(collector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */

		for (Event event : allEventsCollector.getEvents()) {
			System.out.println(event.toString());
		}

		// Not having an Exception here is already good :-)
		Assertions.assertEquals(3, collector.events.size(), "wrong number of link enter events.");
//		Assert.assertEquals("wrong link in first event.", f.link2.getId(), collector.events.get(0).getLinkId());
//		Assert.assertEquals("wrong link in 2nd event.", f.link3.getId(), collector.events.get(1).getLinkId());
	}

	/**
	 * In some cases, the time information of linkEnter/linkLeave events was wrong.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testDeterministicCorrectTiming(boolean isDeterministic) {
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2900, 29), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop1.setLinkId(f.link1.getId());
		stop2.setLinkId(f.link3.getId());
		stop3.setLinkId(f.link3.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId()), f.link3.getId());
		List<TransitRouteStop> stops = List.of(
				new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
				new TransitRouteStopImpl.Builder().stop(stop2).departureOffset(300).build(),
				new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
		);
		TransitRoute route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
		Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 1000);
		dep1.setVehicleId(ptVeh1.getId());
		route1.addDeparture(dep1);
		line1.addRoute(route1);

		schedule.addTransitLine(line1);

		// add a single person with leg from link1 to link3
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(900);
		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		leg.setRoute(new DefaultTransitPassengerRoute(f.link1.getId(), f.link3.getId(), stop1.getId(), stop3.getId(), line1.getId(), route1.getId()));
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector allEventsCollector = new EventsCollector();
		events.addHandler(allEventsCollector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */

		List<TransitDriverStartsEvent> transitDriverStartsEvents = new ArrayList<>();
		List<VehicleArrivesAtFacilityEvent> arrivesAtFacilityEvents = new ArrayList<>();
		List<VehicleDepartsAtFacilityEvent> departsAtFacilityEvents = new ArrayList<>();
		List<LinkEnterEvent> linkEnterEvents = new ArrayList<>();
		List<LinkLeaveEvent> linkLeaveEvents = new ArrayList<>();

		for (Event event : allEventsCollector.getEvents()) {
			System.out.println(event.toString());

			if (event instanceof TransitDriverStartsEvent) transitDriverStartsEvents.add((TransitDriverStartsEvent) event);
			if (event instanceof VehicleArrivesAtFacilityEvent) arrivesAtFacilityEvents.add((VehicleArrivesAtFacilityEvent) event);
			if (event instanceof VehicleDepartsAtFacilityEvent) departsAtFacilityEvents.add((VehicleDepartsAtFacilityEvent) event);
			if (event instanceof LinkEnterEvent) linkEnterEvents.add((LinkEnterEvent) event);
			if (event instanceof LinkLeaveEvent) linkLeaveEvents.add((LinkLeaveEvent) event);
		}

		Assertions.assertEquals(1, transitDriverStartsEvents.size(), "wrong number of transit-driver-starts events.");
		Assertions.assertEquals(2, linkEnterEvents.size(), "wrong number of link enter events.");
		Assertions.assertEquals(2, linkLeaveEvents.size(), "wrong number of link leave events.");
		Assertions.assertEquals(3, arrivesAtFacilityEvents.size(), "wrong number of VehicleArrivesAtFacilityEvents.");
		Assertions.assertEquals(3, departsAtFacilityEvents.size(), "wrong number of VehicleDepartsAtFacilityEvents.");

		if (isDeterministic) Assertions.assertEquals(997.0, transitDriverStartsEvents.get(0).getTime(), 1e-8);

		if (isDeterministic) Assertions.assertEquals(998.0, arrivesAtFacilityEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(stop1.getId(), arrivesAtFacilityEvents.get(0).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1000.0, departsAtFacilityEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(stop1.getId(), departsAtFacilityEvents.get(0).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1001.0, linkLeaveEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(f.link1.getId(), linkLeaveEvents.get(0).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1001.0, linkEnterEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(f.link2.getId(), linkEnterEvents.get(0).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1251.0, linkLeaveEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(f.link2.getId(), linkLeaveEvents.get(1).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1251.0, linkEnterEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(f.link3.getId(), linkEnterEvents.get(1).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1298.0, arrivesAtFacilityEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(stop2.getId(), arrivesAtFacilityEvents.get(1).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1300.0, departsAtFacilityEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(stop2.getId(), departsAtFacilityEvents.get(1).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1598.0, arrivesAtFacilityEvents.get(2).getTime(), 1e-8);
		Assertions.assertEquals(stop3.getId(), arrivesAtFacilityEvents.get(2).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1600.0, departsAtFacilityEvents.get(2).getTime(), 1e-8);
		Assertions.assertEquals(stop3.getId(), departsAtFacilityEvents.get(2).getFacilityId());
	}

	/**
	 * Tests that event times are correct when a transit route starts with some links before arriving at the first stop.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testDeterministicCorrectTiming_initialLinks(boolean isDeterministic) {
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2900, 29), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop1.setLinkId(f.link1.getId());
		stop2.setLinkId(f.link2.getId());
		stop3.setLinkId(f.link3.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId()), f.link3.getId());
		List<TransitRouteStop> stops = List.of(
				new TransitRouteStopImpl.Builder().stop(stop2).departureOffset(60).build(),
				new TransitRouteStopImpl.Builder().stop(stop3).arrivalOffset(600).build()
		);
		TransitRoute route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
		Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 1000);
		dep1.setVehicleId(ptVeh1.getId());
		route1.addDeparture(dep1);
		line1.addRoute(route1);

		schedule.addTransitLine(line1);

		// add a single person
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(900);
		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		leg.setRoute(new DefaultTransitPassengerRoute(f.link1.getId(), f.link3.getId(), stop2.getId(), stop3.getId(), line1.getId(), route1.getId()));
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector allEventsCollector = new EventsCollector();
		events.addHandler(allEventsCollector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */

		List<TransitDriverStartsEvent> transitDriverStartsEvents = new ArrayList<>();
		List<VehicleArrivesAtFacilityEvent> arrivesAtFacilityEvents = new ArrayList<>();
		List<VehicleDepartsAtFacilityEvent> departsAtFacilityEvents = new ArrayList<>();
		List<LinkEnterEvent> linkEnterEvents = new ArrayList<>();
		List<LinkLeaveEvent> linkLeaveEvents = new ArrayList<>();

		for (Event event : allEventsCollector.getEvents()) {
			System.out.println(event.toString());

			if (event instanceof TransitDriverStartsEvent) transitDriverStartsEvents.add((TransitDriverStartsEvent) event);
			if (event instanceof VehicleArrivesAtFacilityEvent) arrivesAtFacilityEvents.add((VehicleArrivesAtFacilityEvent) event);
			if (event instanceof VehicleDepartsAtFacilityEvent) departsAtFacilityEvents.add((VehicleDepartsAtFacilityEvent) event);
			if (event instanceof LinkEnterEvent) linkEnterEvents.add((LinkEnterEvent) event);
			if (event instanceof LinkLeaveEvent) linkLeaveEvents.add((LinkLeaveEvent) event);
		}

		Assertions.assertEquals(1, transitDriverStartsEvents.size(), "wrong number of transit-driver-starts events.");
		Assertions.assertEquals(2, linkEnterEvents.size(), "wrong number of link enter events.");
		Assertions.assertEquals(2, linkLeaveEvents.size(), "wrong number of link leave events.");
		Assertions.assertEquals(2, arrivesAtFacilityEvents.size(), "wrong number of VehicleArrivesAtFacilityEvents.");
		Assertions.assertEquals(2, departsAtFacilityEvents.size(), "wrong number of VehicleDepartsAtFacilityEvents.");

		Assertions.assertEquals(1000.0, transitDriverStartsEvents.get(0).getTime(), 1e-8);

		if (isDeterministic) Assertions.assertEquals(1001.0, linkLeaveEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(f.link1.getId(), linkLeaveEvents.get(0).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1001.0, linkEnterEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(f.link2.getId(), linkEnterEvents.get(0).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1058.0, arrivesAtFacilityEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(stop2.getId(), arrivesAtFacilityEvents.get(0).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1060.0, departsAtFacilityEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(stop2.getId(), departsAtFacilityEvents.get(0).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1061.0, linkLeaveEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(f.link2.getId(), linkLeaveEvents.get(1).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1061.0, linkEnterEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(f.link3.getId(), linkEnterEvents.get(1).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1598.0, arrivesAtFacilityEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(stop3.getId(), arrivesAtFacilityEvents.get(1).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1600.0, departsAtFacilityEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(stop3.getId(), departsAtFacilityEvents.get(1).getFacilityId());
	}

	/**
	 * Tests that everything is correct when a transit route ends with some links after arriving at the last stop.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testTrailingLinksInRoute(boolean isDeterministic) {
		Fixture f = new Fixture();
		f.config.transit().setUseTransit(true);
		f.config.hermes().setDeterministicPt(isDeterministic);

		Vehicles ptVehicles = f.scenario.getTransitVehicles();

		VehicleType ptVehType1 = ptVehicles.getFactory().createVehicleType(Id.create("bus", VehicleType.class));
		ptVehType1.getCapacity().setSeats(1);
		ptVehicles.addVehicleType(ptVehType1);

		Vehicle ptVeh1 = ptVehicles.getFactory().createVehicle(Id.create("veh1", Vehicle.class), ptVehType1);
		ptVehicles.addVehicle(ptVeh1);

		TransitSchedule schedule = f.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility stop1 = sf.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(1000, 10), false);
		TransitStopFacility stop2 = sf.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(2900, 29), false);
		TransitStopFacility stop3 = sf.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord(3000, 30), false);

		stop1.setLinkId(f.link1.getId());
		stop2.setLinkId(f.link2.getId());
		stop3.setLinkId(f.link3.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);

		TransitLine line1 = sf.createTransitLine(Id.create(1, TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), List.of(f.link2.getId()), f.link3.getId());
		List<TransitRouteStop> stops = List.of(
				new TransitRouteStopImpl.Builder().stop(stop1).departureOffset(0).build(),
				new TransitRouteStopImpl.Builder().stop(stop2).arrivalOffset(300).build()
		);
		TransitRoute route1 = sf.createTransitRoute(Id.create(0, TransitRoute.class), netRoute, stops, "bus");
		Departure dep1 = sf.createDeparture(Id.create("dep1", Departure.class), 1000);
		dep1.setVehicleId(ptVeh1.getId());
		route1.addDeparture(dep1);
		line1.addRoute(route1);

		schedule.addTransitLine(line1);

		// add a single person
		Person person = PopulationUtils.getFactory().createPerson(Id.create(0, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
		a1.setEndTime(900);
		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		leg.setRoute(new DefaultTransitPassengerRoute(f.link1.getId(), f.link2.getId(), stop1.getId(), stop2.getId(), line1.getId(), route1.getId()));
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link2.getId());
		f.plans.addPerson(person);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector allEventsCollector = new EventsCollector();
		events.addHandler(allEventsCollector);

		/* run sim */
		Hermes sim = createHermes(f, events);
		sim.run();

		/* finish */

		List<TransitDriverStartsEvent> transitDriverStartsEvents = new ArrayList<>();
		List<VehicleArrivesAtFacilityEvent> arrivesAtFacilityEvents = new ArrayList<>();
		List<VehicleDepartsAtFacilityEvent> departsAtFacilityEvents = new ArrayList<>();
		List<LinkEnterEvent> linkEnterEvents = new ArrayList<>();
		List<LinkLeaveEvent> linkLeaveEvents = new ArrayList<>();
		List<PersonLeavesVehicleEvent> personLeavesVehicleEvents = new ArrayList<>();

		for (Event event : allEventsCollector.getEvents()) {
			System.out.println(event.toString());

			if (event instanceof TransitDriverStartsEvent) transitDriverStartsEvents.add((TransitDriverStartsEvent) event);
			if (event instanceof VehicleArrivesAtFacilityEvent) arrivesAtFacilityEvents.add((VehicleArrivesAtFacilityEvent) event);
			if (event instanceof VehicleDepartsAtFacilityEvent) departsAtFacilityEvents.add((VehicleDepartsAtFacilityEvent) event);
			if (event instanceof LinkEnterEvent) linkEnterEvents.add((LinkEnterEvent) event);
			if (event instanceof LinkLeaveEvent) linkLeaveEvents.add((LinkLeaveEvent) event);
			if (event instanceof PersonLeavesVehicleEvent) personLeavesVehicleEvents.add((PersonLeavesVehicleEvent) event);
		}

		Assertions.assertEquals(1, transitDriverStartsEvents.size(), "wrong number of transit-driver-starts events.");
		Assertions.assertEquals(2, linkEnterEvents.size(), "wrong number of link enter events.");
		Assertions.assertEquals(2, linkLeaveEvents.size(), "wrong number of link leave events.");
		Assertions.assertEquals(2, arrivesAtFacilityEvents.size(), "wrong number of VehicleArrivesAtFacilityEvents.");
		Assertions.assertEquals(2, departsAtFacilityEvents.size(), "wrong number of VehicleDepartsAtFacilityEvents.");
		Assertions.assertEquals(2, personLeavesVehicleEvents.size(), "wrong number of PersonLeavesVehicleEvents.");

		if (isDeterministic) Assertions.assertEquals(997.0, transitDriverStartsEvents.get(0).getTime(), 1e-8);
		Id<Person> transitDriverId = transitDriverStartsEvents.get(0).getDriverId();

		if (isDeterministic) Assertions.assertEquals(998.0, arrivesAtFacilityEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(stop1.getId(), arrivesAtFacilityEvents.get(0).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1000.0, departsAtFacilityEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(stop1.getId(), departsAtFacilityEvents.get(0).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1001.0, linkLeaveEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(f.link1.getId(), linkLeaveEvents.get(0).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1001.0, linkEnterEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(f.link2.getId(), linkEnterEvents.get(0).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1298.0, arrivesAtFacilityEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(stop2.getId(), arrivesAtFacilityEvents.get(1).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1299.0, personLeavesVehicleEvents.get(0).getTime(), 1e-8);
		Assertions.assertEquals(person.getId(), personLeavesVehicleEvents.get(0).getPersonId());

		if (isDeterministic) Assertions.assertEquals(1300.0, departsAtFacilityEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(stop2.getId(), departsAtFacilityEvents.get(1).getFacilityId());

		if (isDeterministic) Assertions.assertEquals(1301.0, linkLeaveEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(f.link2.getId(), linkLeaveEvents.get(1).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1301.0, linkEnterEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(f.link3.getId(), linkEnterEvents.get(1).getLinkId());

		if (isDeterministic) Assertions.assertEquals(1312.0, personLeavesVehicleEvents.get(1).getTime(), 1e-8);
		Assertions.assertEquals(transitDriverId, personLeavesVehicleEvents.get(1).getPersonId());

	}

	/**
	 * Initializes some commonly used data in the tests.
	 *
	 * @author mrieser
	 */
	public static final class Fixture {
		final Config config;
		final Scenario scenario;
		final Network network;
		final Node node1;
		final Node node2;
		final Node node3;
		final Node node4;
		final Link link1;
		final Link link2;
		final Link link3;
		final Link linkX;
		final Population plans;
		final List<Id<Link>> linkIdsNone;
		final List<Id<Link>> linkIds2;

		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.config = this.scenario.getConfig();


			/* build network */
			this.network = this.scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			this.node1 = NetworkUtils.createAndAddNode(this.network, Id.create("1", Node.class), new Coord(0, 0));
			this.node2 = NetworkUtils.createAndAddNode(this.network, Id.create("2", Node.class), new Coord(100, 0));
			this.node3 = NetworkUtils.createAndAddNode(this.network, Id.create("3", Node.class), new Coord(1100, 0));
			this.node4 = NetworkUtils.createAndAddNode(this.network, Id.create("4", Node.class), new Coord(1200, 0));
			this.link1 = NetworkUtils.createAndAddLink(this.network, Id.create("1", Link.class), this.node1, this.node2, 100, 10, 60000, 9 );
			this.link2 = NetworkUtils.createAndAddLink(this.network, Id.create("2", Link.class), this.node2, this.node3, 1000, 100, 6000, 2 );
			this.link3 = NetworkUtils.createAndAddLink(this.network, Id.create("3", Link.class), this.node3, this.node4, 100, 10, 60000, 9 );
			this.linkX = NetworkUtils.createAndAddLink(this.network, Id.create("X", Link.class), this.node4, this.node1, 2000, 100, 2000, 1 );

			/* build plans */
			this.plans = this.scenario.getPopulation();

			this.linkIdsNone = Collections.emptyList();

			this.linkIds2 = List.of(this.link2.getId());
		}
	}

}
