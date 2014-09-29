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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class TransitStopLoadByTimeTest {

	@Test
	public void testTransitLoad_singleLine() {
		Id<Link>[] id = new Id[4];
		id[0] = Id.create(0, Link.class);
		id[1] = Id.create(1, Link.class);
		id[2] = Id.create(2, Link.class);
		id[3] = Id.create(3, Link.class);

		Id<TransitStopFacility> stopId1 = Id.create(0, TransitStopFacility.class);
		Id<TransitStopFacility> stopId2 = Id.create(1, TransitStopFacility.class);
		Id<Person> agentId1 = Id.create(0, Person.class);
		Id<Vehicle> vehicleIdDep1 = Id.create(0, Vehicle.class);

		TransitStopLoadByTime tl = new TransitStopLoadByTime();
		tl.handleEvent(new ActivityEndEvent(100, agentId1, id[0], null, "home"));
		// departure - walk
		// arrival - walk
		// act start - pt interaction
		tl.handleEvent(new ActivityEndEvent(110, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new VehicleArrivesAtFacilityEvent(130, vehicleIdDep1, stopId1, 0));
		tl.handleEvent(new PersonEntersVehicleEvent(135, agentId1, vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(140, vehicleIdDep1, stopId1, 0));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent(200, vehicleIdDep1, stopId2, 0));
		// personLeavesVehicle
		// act start - pt interaction
		tl.handleEvent(new ActivityEndEvent(210, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		// departure - walk
		// arrival - walk
		// act start - work
		tl.handleEvent(new ActivityEndEvent(500, agentId1, id[0], null, "work"));
		// departure - walk
		// arrival - walk
		// act start - pt interaction
		tl.handleEvent(new ActivityEndEvent(530, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new VehicleArrivesAtFacilityEvent(550, vehicleIdDep1, stopId2, 0));
		tl.handleEvent(new PersonEntersVehicleEvent(555, agentId1, vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(570, vehicleIdDep1, stopId2, 0));

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
		Id<Link>[] id = new Id[4];
		id[0] = Id.create(0, Link.class);
		id[1] = Id.create(1, Link.class);
		id[2] = Id.create(2, Link.class);
		id[3] = Id.create(3, Link.class);

		Id<TransitStopFacility> stopId1 = Id.create(0, TransitStopFacility.class);
		Id<Person> agentId1 = Id.create(0, Person.class);
		Id<Person> agentId2 = Id.create(1, Person.class);
		Id<Vehicle> vehicleIdDep1 = Id.create(0, Vehicle.class);
		Id<Vehicle> vehicleIdDep2 = Id.create(0, Vehicle.class);

		TransitStopLoadByTime tl = new TransitStopLoadByTime();
		tl.handleEvent(new ActivityEndEvent(110, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new ActivityEndEvent(120, agentId2, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new VehicleArrivesAtFacilityEvent(130, vehicleIdDep2, stopId1, 0));
		tl.handleEvent(new PersonEntersVehicleEvent(135, agentId2, vehicleIdDep2));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(140, vehicleIdDep2, stopId1, 0));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent(150, vehicleIdDep1, stopId1, 0));
		tl.handleEvent(new PersonEntersVehicleEvent(155, agentId1, vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(160, vehicleIdDep1, stopId1, 0));

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
		Id<Link>[] id = new Id[4];
		id[0] = Id.create(0, Link.class);
		id[1] = Id.create(1, Link.class);
		id[2] = Id.create(2, Link.class);
		id[3] = Id.create(3, Link.class);

		Id<TransitStopFacility> stopId1 = Id.create(0, TransitStopFacility.class);
		Id<Person> agentId1 = Id.create(0, Person.class);
		Id<Person> agentId2 = Id.create(1, Person.class);
		Id<Vehicle> vehicleIdDep1 = Id.create(0, Vehicle.class);
		Id<Vehicle> vehicleIdDep2 = Id.create(0, Vehicle.class);

		TransitStopLoadByTime tl = new TransitStopLoadByTime();
		tl.handleEvent(new ActivityEndEvent(110, agentId1, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new ActivityEndEvent(120, agentId2, id[0], null, PtConstants.TRANSIT_ACTIVITY_TYPE));
		tl.handleEvent(new VehicleArrivesAtFacilityEvent(130, vehicleIdDep2, stopId1, 0));
		tl.handleEvent(new PersonEntersVehicleEvent(135, agentId2, vehicleIdDep2));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(140, vehicleIdDep2, stopId1, 0));

		tl.handleEvent(new VehicleArrivesAtFacilityEvent(150, vehicleIdDep1, stopId1, 0));
		tl.handleEvent(new PersonEntersVehicleEvent(155, agentId1, vehicleIdDep1));
		tl.handleEvent(new VehicleDepartsAtFacilityEvent(160, vehicleIdDep1, stopId1, 0));

		Map<Double, Integer> map = tl.getStopFacilityLoad(stopId1);

		Assert.assertEquals(4, map.size());
		Assert.assertEquals(1, map.get(110.0).intValue());
		Assert.assertEquals(2, map.get(120.0).intValue());
		Assert.assertEquals(1, map.get(135.0).intValue());
		Assert.assertEquals(0, map.get(155.0).intValue());
	}
}
