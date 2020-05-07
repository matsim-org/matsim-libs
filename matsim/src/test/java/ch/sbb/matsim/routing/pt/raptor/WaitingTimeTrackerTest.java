package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.WaitingTimeTracker.DepartureData;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser / Simunto GmbH
 */
public class WaitingTimeTrackerTest {

	@Test
	public void testGetNextDeparture() {
		Id<Vehicle> veh0 = Id.create("veh0", Vehicle.class);
		Id<Vehicle> veh1 = Id.create("veh1", Vehicle.class);
		Id<Vehicle> veh2 = Id.create("veh2", Vehicle.class);
		Id<Vehicle> veh3 = Id.create("veh3", Vehicle.class);
		Id<Vehicle> veh4 = Id.create("veh4", Vehicle.class);
		Id<Vehicle> veh5 = Id.create("veh5", Vehicle.class);

		Id<Departure> dep0 = Id.create("dep0", Departure.class);
		Id<Departure> dep1 = Id.create("dep1", Departure.class);
		Id<Departure> dep2 = Id.create("dep2", Departure.class);
		Id<Departure> dep3 = Id.create("dep3", Departure.class);
		Id<Departure> dep4 = Id.create("dep4", Departure.class);
		Id<Departure> dep5 = Id.create("dep5", Departure.class);

		Id<TransitLine> line1 = Id.create("line1", TransitLine.class);
		Id<TransitRoute> route1 = Id.create("route1", TransitRoute.class);

		Id<TransitStopFacility> stop1 = Id.create("stop1", TransitStopFacility.class);
		Id<TransitStopFacility> stop2 = Id.create("stop2", TransitStopFacility.class);

		Id<Person> driver0 = Id.create("driv0", Person.class);
		Id<Person> driver1 = Id.create("driv1", Person.class);
		Id<Person> driver2 = Id.create("driv2", Person.class);
		Id<Person> driver3 = Id.create("driv3", Person.class);
		Id<Person> driver4 = Id.create("driv4", Person.class);
		Id<Person> driver5 = Id.create("driv5", Person.class);

		Id<Person> pax1 = Id.create("pax1", Person.class);
		Id<Person> pax2 = Id.create("pax2", Person.class);
		Id<Person> pax3 = Id.create("pax3", Person.class);
		Id<Person> pax4 = Id.create("pax4", Person.class);
		Id<Person> pax5 = Id.create("pax5", Person.class);
		Id<Person> pax6 = Id.create("pax6", Person.class);
		Id<Person> pax7 = Id.create("pax7", Person.class);
		Id<Person> pax8 = Id.create("pax8", Person.class);

		/*
		 *       7:00      7:10      7:20      7:30      7:40      7:50      8:00
		 *        |---------|---------|---------|---------|---------|---------|----> time
		 *
		 *   dep: ^         ^         ^         ^         ^         ^         ^
		 *        0         1         2         3         4         5         6
		 *
		 *   pax:    ^  ^ ^  ^    ^ ^      ^  ^
		 *           1  2 3  4    5 6      7  8
		 *
		 *              |    |             ||                       |         |
		 * pax-dep    1 |  2 |       3     ||           5           |    6    |
		 *              |    |             ||                       |         |
		 */

		WaitingTimeTracker tracker = new WaitingTimeTracker();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(tracker);

		events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:20:00"), driver0, veh0, line1, route1, dep0));
		events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:30:00"), driver1, veh1, line1, route1, dep1));
		events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:40:00"), driver2, veh2, line1, route1, dep2));
		events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:50:00"), driver3, veh3, line1, route1, dep3));
		events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:55:00"), driver4, veh4, line1, route1, dep4));
		events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:59:00"), driver5, veh5, line1, route1, dep5));

		events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:00:00"), veh0, stop1, 0.0));
		events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:00:30"), veh0, stop1, 0.0));

		events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:03:00"), pax1, stop1, stop2));
		events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:06:00"), pax2, stop1, stop2));
		events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:08:00"), pax3, stop1, stop2));

		events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:10:00"), veh1, stop1, 0.0));
		events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:10"), pax1, veh1));
		events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:20"), pax2, veh1));
		events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:10:30"), veh1, stop1, 0.0));

		events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:11:00"), pax4, stop1, stop2));
		events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:16:00"), pax5, stop1, stop2));
		events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:18:00"), pax6, stop1, stop2));

		events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:20:00"), veh2, stop1, 0.0));
		events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:10"), pax3, veh2));
		events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:20"), pax4, veh2));
		events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:20:30"), veh2, stop1, 0.0));

		events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:24:00"), pax7, stop1, stop2));
		events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:27:00"), pax8, stop1, stop2));

		events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:30:00"), veh3, stop1, 0.0));
		events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:30:05"), pax5, veh3));
		events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:30:15"), pax6, veh3));
		events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:30:25"), pax7, veh3));
		events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:30:30"), veh3, stop1, 0.0));

		events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:40:00"), veh4, stop1, 0.0));
		// veh4 is completely full, nobody can enter
		events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:40:10"), veh4, stop1, 0.0));

		events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:50:00"), veh5, stop1, 0.0));
		events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:50:10"), pax8, veh5));
		events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:50:20"), veh5, stop1, 0.0));

		events.finishProcessing();

		Id<TransitLine> line2 = Id.create("line2", TransitLine.class);
		Id<TransitRoute> route2 = Id.create("route2", TransitRoute.class);

		DepartureData data;
		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("06:45:00")); // before there is any vehicle
		Assert.assertEquals(dep0, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("06:55:00")); // before we have any pax observations
		Assert.assertEquals(dep0, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:00:00"));
		Assert.assertEquals(dep0, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:00:30"));
		Assert.assertEquals(dep0, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:00:31"));
		Assert.assertEquals(dep1, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:05:00"));
		Assert.assertEquals(dep1, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:06:00"));
		Assert.assertEquals(dep1, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:06:01"));
		Assert.assertEquals(dep2, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:11:00"));
		Assert.assertEquals(dep2, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:11:01"));
		Assert.assertEquals(dep3, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:18:00"));
		Assert.assertEquals(dep3, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:18:05"));
		Assert.assertEquals(dep3, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:22:00"));
		Assert.assertEquals(dep3, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:24:00"));
		Assert.assertEquals(dep3, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:24:03"));
		Assert.assertEquals(dep5, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:29:00"));
		Assert.assertEquals(dep5, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:35:00"));
		Assert.assertEquals(dep5, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:40:00"));
		Assert.assertEquals(dep5, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:50:00"));
		Assert.assertEquals(dep5, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:50:05"));
		Assert.assertEquals(dep5, data.departureId);

		data = tracker.getNextAvailableDeparture(line1, route1, stop1, Time.parseTime("07:50:30"));
		Assert.assertNull(data);
	}

}
