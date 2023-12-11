/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.pt.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class TransitLoadTest {

	@Test
	void testTransitLoad_singleLine() {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = factory.createTransitSchedule();
		TransitStopFacility stop1 = factory.createTransitStopFacility(Id.create(0, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stop2 = factory.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stop3 = factory.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stop4 = factory.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		Collections.addAll(stops, factory.createTransitRouteStop(stop1, 0, 0)
				, factory.createTransitRouteStop(stop2, 360, 360)
				, factory.createTransitRouteStop(stop3, 360, 360)
				, factory.createTransitRouteStop(stop4, 360, 360)
				// add first stop again to check whether serving the same stop multiple times is counted right
				, factory.createTransitRouteStop(stop1, 420, 420));
		TransitLine line1 = factory.createTransitLine(Id.create(1, TransitLine.class));
		TransitRoute route1 = factory.createTransitRoute(Id.create(1, TransitRoute.class), null, stops, "bus");
		Departure dep1 = factory.createDeparture(Id.create(1, Departure.class), 7.0*3600);
		Departure dep2 = factory.createDeparture(Id.create(2, Departure.class), 8.0*3600);
		Id<Vehicle> vehicleIdDep1 = Id.create(0, Vehicle.class);
		Id<Vehicle> vehicleIdDep2 = Id.create(3, Vehicle.class);
		dep1.setVehicleId(vehicleIdDep1);
		dep2.setVehicleId(vehicleIdDep2);
		route1.addDeparture(dep1);
		route1.addDeparture(dep2);
		line1.addRoute(route1);
		schedule.addTransitLine(line1);

		TransitLoad tl = new TransitLoad();

		tl.handleEvent(new TransitDriverStartsEvent(7.0*3600-20, Id.create("ptDriver1", Person.class), vehicleIdDep1, line1.getId(), route1.getId(), dep1.getId()));
		tl.handleEvent(new PersonEntersVehicleEvent(7.0*3600-20, Id.create("ptDriver1", Person.class), vehicleIdDep1));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent(7.0*3600-10, vehicleIdDep1, stop1.getId(), 0));
		tl.handleEvent(new PersonEntersVehicleEvent(7.0*3600-5, Id.create(0, Person.class), vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(7.0*3600+10, vehicleIdDep1, stop1.getId(), 0));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent(7.1*3600-25, vehicleIdDep1, stop2.getId(), 0));
		tl.handleEvent(new PersonLeavesVehicleEvent(7.1*3600-5, Id.create(0, Person.class), vehicleIdDep1));
		tl.handleEvent(new PersonEntersVehicleEvent(7.1*3600, Id.create(1, Person.class), vehicleIdDep1));
		tl.handleEvent(new PersonEntersVehicleEvent(7.1*3600+5, Id.create(2, Person.class), vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(7.1*3600+25, vehicleIdDep1, stop2.getId(), 0));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent(7.2*3600-15, vehicleIdDep1, stop3.getId(), 0));
		tl.handleEvent(new PersonLeavesVehicleEvent(7.2*3600-5, Id.create(2, Person.class), vehicleIdDep1));
		tl.handleEvent(new PersonEntersVehicleEvent(7.2*3600, Id.create(3, Person.class), vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(7.2*3600+20, vehicleIdDep1, stop3.getId(), 0));

		tl.handleEvent(new PersonEntersVehicleEvent(7.25*3600, Id.create("carDriver1", Person.class), Id.create("car1", Vehicle.class)));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent(7.3*3600-20, vehicleIdDep1, stop4.getId(), 0));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(7.3*3600+5, vehicleIdDep1, stop4.getId(), 0));
		
		tl.handleEvent(new PersonLeavesVehicleEvent(7.35*3600-5, Id.create("carDriver1", Person.class), Id.create("car1", Vehicle.class)));
		
		tl.handleEvent(new VehicleArrivesAtFacilityEvent(7.4*3600-20, vehicleIdDep1, stop1.getId(), 0));
		tl.handleEvent(new PersonLeavesVehicleEvent(7.4*3600-5, Id.create(1, Person.class), vehicleIdDep1));
		tl.handleEvent(new PersonLeavesVehicleEvent(7.4*3600, Id.create(3, Person.class), vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(7.4*3600+5, vehicleIdDep1, stop1.getId(), 0));
		
		tl.handleEvent(new PersonLeavesVehicleEvent(7.45*3600-20, Id.create("ptDriver1", Person.class), vehicleIdDep1));


		Assertions.assertEquals(1, tl.getLoadAtDeparture(line1, route1, stop1, dep1));
		Assertions.assertEquals(2, tl.getLoadAtDeparture(line1, route1, stop2, dep1));
		Assertions.assertEquals(2, tl.getLoadAtDeparture(line1, route1, stop3, dep1));
		Assertions.assertEquals(2, tl.getLoadAtDeparture(line1, route1, stop4, dep1));
		
		Assertions.assertEquals(1, tl.getLoadAtDeparture(line1, route1, 0, dep1));
		Assertions.assertEquals(2, tl.getLoadAtDeparture(line1, route1, 1, dep1));
		Assertions.assertEquals(2, tl.getLoadAtDeparture(line1, route1, 2, dep1));
		Assertions.assertEquals(2, tl.getLoadAtDeparture(line1, route1, 3, dep1));
		Assertions.assertEquals(0, tl.getLoadAtDeparture(line1, route1, 4, dep1));

		TransitLoad.StopInformation si = tl.getDepartureStopInformation(line1, route1, stop1, dep1).get(0);
		Assertions.assertEquals(7.0*3600-10, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(7.0*3600+10, si.departureTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, si.nOfEntering);
		Assertions.assertEquals(0, si.nOfLeaving);

		si = tl.getDepartureStopInformation(line1, route1, stop2, dep1).get(0);
		Assertions.assertEquals(7.1*3600-25, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(7.1*3600+25, si.departureTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2, si.nOfEntering);
		Assertions.assertEquals(1, si.nOfLeaving);

		si = tl.getDepartureStopInformation(line1, route1, stop3, dep1).get(0);
		Assertions.assertEquals(7.2*3600-15, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(7.2*3600+20, si.departureTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, si.nOfEntering);
		Assertions.assertEquals(1, si.nOfLeaving);

		si = tl.getDepartureStopInformation(line1, route1, stop4, dep1).get(0);
		Assertions.assertEquals(7.3*3600-20, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(7.3*3600+5, si.departureTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(0, si.nOfEntering);
		Assertions.assertEquals(0, si.nOfLeaving);
		
		si = tl.getDepartureStopInformation(line1, route1, stop1, dep1).get(1);
		Assertions.assertEquals(7.4*3600-20, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(7.4*3600+5, si.departureTime, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(0, si.nOfEntering);
		Assertions.assertEquals(2, si.nOfLeaving);

		List<TransitLoad.StopInformation> siList = tl.getDepartureStopInformation(line1, route1, stop1, dep2);
		Assertions.assertNull(siList);
	}
}
