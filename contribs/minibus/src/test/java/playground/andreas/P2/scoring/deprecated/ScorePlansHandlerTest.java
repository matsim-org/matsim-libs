/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.andreas.P2.scoring.deprecated;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConfigGroup;


public class ScorePlansHandlerTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testScoreContainer() {
		Network net = PScenarioHelper.createTestNetwork().getNetwork();
		PConfigGroup pC = new PConfigGroup();
		pC.addParam("costPerVehicleAndDay", "40.0");
		pC.addParam("earningsPerKilometerAndPassenger", "0.20");
		pC.addParam("costPerKilometer", "0.30");
		
		ScorePlansHandler handler = new ScorePlansHandler(pC);
		handler.init(net);
		
		Id driverId = new IdImpl("drv_1");
		Id vehicleId = new IdImpl("veh_1");
		Id personId = new IdImpl("p_1");
		Id transitLineId = new IdImpl("A");
		Id transitRouteId = new IdImpl("123");
		Id departureId = new IdImpl("dep_1");
		
		ScoreContainer sC;
		
		handler.handleEvent(new TransitDriverStartsEvent(0.0, driverId, vehicleId, transitLineId, transitRouteId, departureId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
		Assert.assertEquals("revenue with zero trips served", -40.0, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.handleEvent(new LinkEnterEvent(0.0, driverId, new IdImpl("1112"), vehicleId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
//		Assert.assertEquals("revenue with zero trips served", -40.36, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue with zero trips served", -40.36002109241936, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		// (disturbing the link lengths (see PScenarioHelper) also changes this value a bit. kai, oct'13)

		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.handleEvent(new PersonEntersVehicleEvent(0.0, personId, vehicleId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
//		Assert.assertEquals("revenue with zero trips served", -40.36, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue with zero trips served", -40.36002109241936, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
//		(same)
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.handleEvent(new LinkEnterEvent(0.0, driverId, new IdImpl("1211"), vehicleId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
//		Assert.assertEquals("revenue with zero trips served", -40.72, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue with zero trips served", -40.720185138621424, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		// (same)
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.handleEvent(new PersonLeavesVehicleEvent(0.0, personId, vehicleId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
//		Assert.assertEquals("revenue with zero trips served", -40.72, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("revenue with zero trips served", -40.720185138621424, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		// (same)
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.reset(10);
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
		Assert.assertNull("There is no score after the reset is triggered", sC);
	}
}