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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.PtConstants;

public class TransitStopLoadByTimeTest {

	@Test
	public void testTransitLoad_singleLine() {
		Id[] id = {new IdImpl(0), new IdImpl(1), new IdImpl(2), new IdImpl(3)};

		Id stopId1 = id[0];
		Id stopId2 = id[1];
		Id agentId1 = id[0];
		Id vehicleIdDep1 = id[0];

		TransitStopLoadByTime tl = new TransitStopLoadByTime();
		tl.handleEvent(new ActivityEndEvent((double) 100, id[0], id[0], null, "home"));
		// departure - walk
		// arrival - walk
		// act start - pt interaction
		tl.handleEvent(new ActivityEndEvent((double) 110, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new VehicleArrivesAtFacilityEvent((double) 130, vehicleIdDep1, stopId1, (double) 0));
		tl.handleEvent(new PersonEntersVehicleEvent((double) 135, agentId1, vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent((double) 140, vehicleIdDep1, stopId1, (double) 0));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent((double) 200, vehicleIdDep1, stopId2, (double) 0));
		// personLeavesVehicle
		// act start - pt interaction
		tl.handleEvent(new ActivityEndEvent((double) 210, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		// departure - walk
		// arrival - walk
		// act start - work
		tl.handleEvent(new ActivityEndEvent((double) 500, agentId1, id[0], null, "work"));
		// departure - walk
		// arrival - walk
		// act start - pt interaction
		tl.handleEvent(new ActivityEndEvent((double) 530, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new VehicleArrivesAtFacilityEvent((double) 550, vehicleIdDep1, stopId2, (double) 0));
		tl.handleEvent(new PersonEntersVehicleEvent((double) 555, agentId1, vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent((double) 570, vehicleIdDep1, stopId2, (double) 0));

		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1,  90));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 100));
		Assert.assertEquals(1, tl.getStopFacilityLoad(stopId1, 110));
		Assert.assertEquals(1, tl.getStopFacilityLoad(stopId1, 130));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 135));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 140));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 200));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 210));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 500));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 530));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 550));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 555));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 570));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 600));

		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2,  90));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 100));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 110));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 130));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 135));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 140));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 200));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 210));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 500));
		Assert.assertEquals(1, tl.getStopFacilityLoad(stopId2, 530));
		Assert.assertEquals(1, tl.getStopFacilityLoad(stopId2, 550));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 555));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 570));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId2, 600));
	}

	@Test
	public void testTransitLoad_twoLines() {
		Id[] id = {new IdImpl(0), new IdImpl(1), new IdImpl(2), new IdImpl(3)};

		Id stopId1 = id[0];
		Id agentId1 = id[0];
		Id agentId2 = id[1];
		Id vehicleIdDep1 = id[0];
		Id vehicleIdDep2 = id[1];

		TransitStopLoadByTime tl = new TransitStopLoadByTime();
		tl.handleEvent(new ActivityEndEvent((double) 110, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new ActivityEndEvent((double) 120, agentId2, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new VehicleArrivesAtFacilityEvent((double) 130, vehicleIdDep2, stopId1, (double) 0));
		tl.handleEvent(new PersonEntersVehicleEvent((double) 135, agentId2, vehicleIdDep2));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent((double) 140, vehicleIdDep2, stopId1, (double) 0));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent((double) 150, vehicleIdDep1, stopId1, (double) 0));
		tl.handleEvent(new PersonEntersVehicleEvent((double) 155, agentId1, vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent((double) 160, vehicleIdDep1, stopId1, (double) 0));

		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1,  90));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 100));
		Assert.assertEquals(1, tl.getStopFacilityLoad(stopId1, 110));
		Assert.assertEquals(2, tl.getStopFacilityLoad(stopId1, 120));
		Assert.assertEquals(2, tl.getStopFacilityLoad(stopId1, 130));
		Assert.assertEquals(1, tl.getStopFacilityLoad(stopId1, 135));
		Assert.assertEquals(1, tl.getStopFacilityLoad(stopId1, 140));
		Assert.assertEquals(1, tl.getStopFacilityLoad(stopId1, 150));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 155));
		Assert.assertEquals(0, tl.getStopFacilityLoad(stopId1, 160));
	}
	@Test
	public void testGetTransitLoad_twoLines() {
		Id[] id = {new IdImpl(0), new IdImpl(1), new IdImpl(2), new IdImpl(3)};

		Id stopId1 = id[0];
		Id agentId1 = id[0];
		Id agentId2 = id[1];
		Id vehicleIdDep1 = id[0];
		Id vehicleIdDep2 = id[1];

		TransitStopLoadByTime tl = new TransitStopLoadByTime();
		tl.handleEvent(new ActivityEndEvent((double) 110, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new ActivityEndEvent((double) 120, agentId2, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new VehicleArrivesAtFacilityEvent((double) 130, vehicleIdDep2, stopId1, (double) 0));
		tl.handleEvent(new PersonEntersVehicleEvent((double) 135, agentId2, vehicleIdDep2));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent((double) 140, vehicleIdDep2, stopId1, (double) 0));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent((double) 150, vehicleIdDep1, stopId1, (double) 0));
		tl.handleEvent(new PersonEntersVehicleEvent((double) 155, agentId1, vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent((double) 160, vehicleIdDep1, stopId1, (double) 0));

		Map<Double, Integer> map = tl.getStopFacilityLoad(stopId1);

		Assert.assertEquals(4, map.size());
		Assert.assertEquals(1, map.get(110.0).intValue());
		Assert.assertEquals(2, map.get(120.0).intValue());
		Assert.assertEquals(1, map.get(135.0).intValue());
		Assert.assertEquals(0, map.get(155.0).intValue());
	}
}
