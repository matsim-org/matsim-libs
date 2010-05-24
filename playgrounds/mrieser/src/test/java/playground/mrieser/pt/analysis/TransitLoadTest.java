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

package playground.mrieser.pt.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class TransitLoadTest {

	@Test
	public void testTransitLoad_singleLine() {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = factory.createTransitSchedule();
		Id[] id = {new IdImpl(0), new IdImpl(1), new IdImpl(2), new IdImpl(3)};
		TransitStopFacility stop1 = factory.createTransitStopFacility(id[0], new CoordImpl(0, 0), false);
		TransitStopFacility stop2 = factory.createTransitStopFacility(id[1], new CoordImpl(0, 0), false);
		TransitStopFacility stop3 = factory.createTransitStopFacility(id[2], new CoordImpl(0, 0), false);
		TransitStopFacility stop4 = factory.createTransitStopFacility(id[3], new CoordImpl(0, 0), false);
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		Collections.addAll(stops, factory.createTransitRouteStop(stop1, 0, 0)
				, factory.createTransitRouteStop(stop2, 360, 360)
				, factory.createTransitRouteStop(stop3, 360, 360)
				, factory.createTransitRouteStop(stop4, 360, 360));
		TransitLine line1 = factory.createTransitLine(id[1]);
		TransitRoute route1 = factory.createTransitRoute(id[1], null, stops, TransportMode.bus);
		Departure dep1 = factory.createDeparture(id[1], 7.0*3600);
		Departure dep2 = factory.createDeparture(id[2], 8.0*3600);
		Id vehicleIdDep1 = id[0];
		Id vehicleIdDep2 = id[3];
		dep1.setVehicleId(vehicleIdDep1);
		dep2.setVehicleId(vehicleIdDep2);
		route1.addDeparture(dep1);
		route1.addDeparture(dep2);
		line1.addRoute(route1);
		schedule.addTransitLine(line1);

		EventsFactoryImpl ef = new EventsFactoryImpl();

		TransitLoad tl = new TransitLoad(schedule);
		tl.handleEvent(ef.createVehicleArrivesAtFacilityEvent(7.0*3600-10, vehicleIdDep1, stop1.getId(), 0));
		tl.handleEvent(ef.createPersonEntersVehicleEvent(7.0*3600-5, id[0], vehicleIdDep1, route1.getId()));
		tl.handleEvent(ef.createVehicleDepartsAtFacilityEvent(7.0*3600+10, vehicleIdDep1, stop1.getId(), 0));

		tl.handleEvent(ef.createVehicleArrivesAtFacilityEvent(7.1*3600-25, vehicleIdDep1, stop2.getId(), 0));
		tl.handleEvent(ef.createPersonLeavesVehicleEvent(7.1*3600-5, id[0], vehicleIdDep1, route1.getId()));
		tl.handleEvent(ef.createPersonEntersVehicleEvent(7.1*3600, id[1], vehicleIdDep1, route1.getId()));
		tl.handleEvent(ef.createPersonEntersVehicleEvent(7.1*3600+5, id[2], vehicleIdDep1, route1.getId()));
		tl.handleEvent(ef.createVehicleDepartsAtFacilityEvent(7.1*3600+25, vehicleIdDep1, stop2.getId(), 0));

		tl.handleEvent(ef.createVehicleArrivesAtFacilityEvent(7.2*3600-15, vehicleIdDep1, stop3.getId(), 0));
		tl.handleEvent(ef.createPersonLeavesVehicleEvent(7.2*3600-5, id[2], vehicleIdDep1, route1.getId()));
		tl.handleEvent(ef.createPersonEntersVehicleEvent(7.2*3600, id[3], vehicleIdDep1, route1.getId()));
		tl.handleEvent(ef.createVehicleDepartsAtFacilityEvent(7.2*3600+20, vehicleIdDep1, stop3.getId(), 0));

		tl.handleEvent(ef.createVehicleArrivesAtFacilityEvent(7.3*3600-20, vehicleIdDep1, stop4.getId(), 0));
		tl.handleEvent(ef.createPersonLeavesVehicleEvent(7.3*3600-5, id[1], vehicleIdDep1, route1.getId()));
		tl.handleEvent(ef.createPersonLeavesVehicleEvent(7.3*3600, id[3], vehicleIdDep1, route1.getId()));
		tl.handleEvent(ef.createVehicleDepartsAtFacilityEvent(7.3*3600+5, vehicleIdDep1, stop4.getId(), 0));

		Assert.assertEquals(1, tl.getLoadAtDeparture(line1, route1, stop1, dep1));
		Assert.assertEquals(2, tl.getLoadAtDeparture(line1, route1, stop2, dep1));
		Assert.assertEquals(2, tl.getLoadAtDeparture(line1, route1, stop3, dep1));
		Assert.assertEquals(0, tl.getLoadAtDeparture(line1, route1, stop4, dep1));

		TransitLoad.StopInformation si = tl.getDepartureStopInformation(line1, route1, stop1, dep1);
		Assert.assertEquals(7.0*3600-10, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assert.assertEquals(7.0*3600+10, si.departureTime, MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, si.nOfEntering);
		Assert.assertEquals(0, si.nOfLeaving);

		si = tl.getDepartureStopInformation(line1, route1, stop2, dep1);
		Assert.assertEquals(7.1*3600-25, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assert.assertEquals(7.1*3600+25, si.departureTime, MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, si.nOfEntering);
		Assert.assertEquals(1, si.nOfLeaving);

		si = tl.getDepartureStopInformation(line1, route1, stop3, dep1);
		Assert.assertEquals(7.2*3600-15, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assert.assertEquals(7.2*3600+20, si.departureTime, MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, si.nOfEntering);
		Assert.assertEquals(1, si.nOfLeaving);

		si = tl.getDepartureStopInformation(line1, route1, stop4, dep1);
		Assert.assertEquals(7.3*3600-20, si.arrivalTime, MatsimTestUtils.EPSILON);
		Assert.assertEquals(7.3*3600+5, si.departureTime, MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, si.nOfEntering);
		Assert.assertEquals(2, si.nOfLeaving);

		si = tl.getDepartureStopInformation(line1, route1, stop1, dep2);
		Assert.assertNull(si);
	}
}
