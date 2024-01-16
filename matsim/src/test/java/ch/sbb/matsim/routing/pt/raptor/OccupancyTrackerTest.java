/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.OccupancyData.DepartureData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.Collections;

/**
 * @author mrieser / Simunto GmbH
 */
public class OccupancyTrackerTest {

	@Test
	void testGetNextDeparture() {
		Fixture f = new Fixture();

		EventsManager events = EventsUtils.createEventsManager();
		OccupancyData occData = new OccupancyData();
		OccupancyTracker tracker = new OccupancyTracker(occData, f.scenario, new DefaultRaptorInVehicleCostCalculator(), events, new SubpopulationScoringParameters(f.scenario));
		events.addHandler(tracker);

		events.initProcessing();
		f.generateEvents(events);

		DepartureData data;
		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("06:45:00")); // before there is any vehicle
		Assertions.assertEquals(f.dep0, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("06:55:00")); // before we have any pax observations
		Assertions.assertEquals(f.dep0, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:00:00"));
		Assertions.assertEquals(f.dep0, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:00:30"));
		Assertions.assertEquals(f.dep0, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:00:31"));
		Assertions.assertEquals(f.dep1, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:05:00"));
		Assertions.assertEquals(f.dep1, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:06:00"));
		Assertions.assertEquals(f.dep1, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:06:01"));
		Assertions.assertEquals(f.dep2, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:11:00"));
		Assertions.assertEquals(f.dep2, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:11:01"));
		Assertions.assertEquals(f.dep3, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:18:00"));
		Assertions.assertEquals(f.dep3, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:18:05"));
		Assertions.assertEquals(f.dep3, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:22:00"));
		Assertions.assertEquals(f.dep3, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:24:00"));
		Assertions.assertEquals(f.dep3, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:24:03"));
		Assertions.assertEquals(f.dep5, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:29:00"));
		Assertions.assertEquals(f.dep5, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:35:00"));
		Assertions.assertEquals(f.dep5, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:40:00"));
		Assertions.assertEquals(f.dep5, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:50:00"));
		Assertions.assertEquals(f.dep5, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:50:05"));
		Assertions.assertEquals(f.dep5, data.departureId);

		data = occData.getNextAvailableDeparture(f.line1, f.route1, f.stop1, Time.parseTime("07:50:30"));
		Assertions.assertNull(data);
	}

	@Test
	void testGetDepartureData() {
		Fixture f = new Fixture();

		EventsManager events = EventsUtils.createEventsManager();
		OccupancyData occData = new OccupancyData();
		OccupancyTracker tracker = new OccupancyTracker(occData, f.scenario, new DefaultRaptorInVehicleCostCalculator(), events, new SubpopulationScoringParameters(f.scenario));
		events.addHandler(tracker);

		events.initProcessing();
		f.generateEvents(events);

		DepartureData data;
		data = occData.getDepartureData(f.line1, f.route1, f.stop1, f.dep0);
		Assertions.assertEquals(0, data.paxCountAtDeparture);

		data = occData.getDepartureData(f.line1, f.route1, f.stop1, f.dep1);
		Assertions.assertEquals(2, data.paxCountAtDeparture);

		data = occData.getDepartureData(f.line1, f.route1, f.stop1, f.dep2);
		Assertions.assertEquals(2, data.paxCountAtDeparture);

		data = occData.getDepartureData(f.line1, f.route1, f.stop1, f.dep3);
		Assertions.assertEquals(3, data.paxCountAtDeparture);

		data = occData.getDepartureData(f.line1, f.route1, f.stop1, f.dep4);
		Assertions.assertEquals(0, data.paxCountAtDeparture);

		data = occData.getDepartureData(f.line1, f.route1, f.stop1, f.dep5);
		Assertions.assertEquals(1, data.paxCountAtDeparture);
	}

	private static class Fixture {

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

		Scenario scenario;

		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			TransitSchedule schedule = this.scenario.getTransitSchedule();
			TransitScheduleFactory factory = schedule.getFactory();
			TransitLine line = factory.createTransitLine(this.line1);
			schedule.addTransitLine(line);
			TransitRoute route = factory.createTransitRoute(this.route1, null, Collections.emptyList(), "bus");
			line.addRoute(route);
		}

		public void generateEvents(EventsManager events) {

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

			Id<Link> linkId = Id.create("1", Link.class);

			events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:20:00"), this.driver0, this.veh0, this.line1, this.route1, this.dep0));
			events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:30:00"), this.driver1, this.veh1, this.line1, this.route1, this.dep1));
			events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:40:00"), this.driver2, this.veh2, this.line1, this.route1, this.dep2));
			events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:50:00"), this.driver3, this.veh3, this.line1, this.route1, this.dep3));
			events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:55:00"), this.driver4, this.veh4, this.line1, this.route1, this.dep4));
			events.processEvent(new TransitDriverStartsEvent(Time.parseTime("06:59:00"), this.driver5, this.veh5, this.line1, this.route1, this.dep5));

			events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:00:00"), this.veh0, this.stop1, 0.0));
			events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:00:30"), this.veh0, this.stop1, 0.0));

			events.processEvent(new PersonDepartureEvent(Time.parseTime("07:03:00"), this.pax1, linkId, "pt", "pt"));
			events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:03:00"), this.pax1, this.stop1, this.stop2));
			events.processEvent(new PersonDepartureEvent(Time.parseTime("07:06:00"), this.pax2, linkId, "pt", "pt"));
			events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:06:00"), this.pax2, this.stop1, this.stop2));
			events.processEvent(new PersonDepartureEvent(Time.parseTime("07:08:00"), this.pax3, linkId, "pt", "pt"));
			events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:08:00"), this.pax3, this.stop1, this.stop2));

			events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:10:00"), this.veh1, this.stop1, 0.0));
			events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:10"), this.pax1, this.veh1));
			events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:10:20"), this.pax2, this.veh1));
			events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:10:30"), this.veh1, this.stop1, 0.0));

			events.processEvent(new PersonDepartureEvent(Time.parseTime("07:11:00"), this.pax4, linkId, "pt", "pt"));
			events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:11:00"), this.pax4, this.stop1, this.stop2));
			events.processEvent(new PersonDepartureEvent(Time.parseTime("07:16:00"), this.pax5, linkId, "pt", "pt"));
			events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:16:00"), this.pax5, this.stop1, this.stop2));
			events.processEvent(new PersonDepartureEvent(Time.parseTime("07:18:00"), this.pax6, linkId, "pt", "pt"));
			events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:18:00"), this.pax6, this.stop1, this.stop2));

			events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:20:00"), this.veh2, this.stop1, 0.0));
			events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:10"), this.pax3, this.veh2));
			events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:20:20"), this.pax4, this.veh2));
			events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:20:30"), this.veh2, this.stop1, 0.0));

			events.processEvent(new PersonDepartureEvent(Time.parseTime("07:24:00"), this.pax7, linkId, "pt", "pt"));
			events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:24:00"), this.pax7, this.stop1, this.stop2));
			events.processEvent(new PersonDepartureEvent(Time.parseTime("07:27:00"), this.pax8, linkId, "pt", "pt"));
			events.processEvent(new AgentWaitingForPtEvent(Time.parseTime("07:27:00"), this.pax8, this.stop1, this.stop2));

			events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:30:00"), this.veh3, this.stop1, 0.0));
			events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:30:05"), this.pax5, this.veh3));
			events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:30:15"), this.pax6, this.veh3));
			events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:30:25"), this.pax7, this.veh3));
			events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:30:30"), this.veh3, this.stop1, 0.0));

			events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:40:00"), this.veh4, this.stop1, 0.0));
			// veh4 is completely full, nobody can enter
			events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:40:10"), this.veh4, this.stop1, 0.0));

			events.processEvent(new VehicleArrivesAtFacilityEvent(Time.parseTime("07:50:00"), this.veh5, this.stop1, 0.0));
			events.processEvent(new PersonEntersVehicleEvent(Time.parseTime("07:50:10"), this.pax8, this.veh5));
			events.processEvent(new VehicleDepartsAtFacilityEvent(Time.parseTime("07:50:20"), this.veh5, this.stop1, 0.0));

			events.finishProcessing();
		}

	}

}
