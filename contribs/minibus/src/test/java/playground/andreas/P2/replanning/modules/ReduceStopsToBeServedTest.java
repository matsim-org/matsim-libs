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

package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.modules.deprecated.ReduceStopsToBeServed;


public class ReduceStopsToBeServedTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		PConfigGroup pC = new PConfigGroup();
		
		Cooperative coop = PScenarioHelper.createCoop2111to1314to4443();
		ArrayList<String> param = new ArrayList<String>();
		param.add("1.0");		
		ReduceStopsToBeServed strat = new ReduceStopsToBeServed(param);
		strat.setPIdentifier(pC.getPIdentifier());
		
		Id stopId1 = new IdImpl(pC.getPIdentifier() + "2111");
		Id stopId2 = new IdImpl(pC.getPIdentifier() + "1314");
		Id stopId3 = new IdImpl(pC.getPIdentifier() + "4443");
		
		Assert.assertEquals("Number of stops to be served", 3, coop.getBestPlan().getStopsToBeServed().size());
		Assert.assertEquals("Start stop", stopId1.toString(), coop.getBestPlan().getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Middle stop", stopId2.toString(), coop.getBestPlan().getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("End stop", stopId3.toString(), coop.getBestPlan().getStopsToBeServed().get(2).getId().toString());
		
		Id veh1 = new IdImpl(pC.getPIdentifier() + 1 + "-" + 1);
		Id lineId = coop.getId();
		
		strat.handleEvent(new TransitDriverStartsEvent(18000.0, new IdImpl("d1"), veh1, lineId, new IdImpl("route1"), new IdImpl("dep1")));
		strat.handleEvent(new VehicleArrivesAtFacilityEvent(18000.0, veh1, stopId1, 0.0));
		

		PPlan testPlan = null;
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start stop", stopId1.toString(), coop.getBestPlan().getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Middle stop", stopId2.toString(), coop.getBestPlan().getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("End stop", stopId3.toString(), coop.getBestPlan().getStopsToBeServed().get(2).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);
		
		// too few vehicles for testing - nothing should change
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start stop", stopId1.toString(), coop.getBestPlan().getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Middle stop", stopId2.toString(), coop.getBestPlan().getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("End stop", stopId3.toString(), coop.getBestPlan().getStopsToBeServed().get(2).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);
		
		coop.getBestPlan().setNVehicles(2);
		
		// enough vehicles for testing but still no demand - nothing should change
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start stop", stopId1.toString(), coop.getBestPlan().getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("Middle stop", stopId2.toString(), coop.getBestPlan().getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("End stop", stopId3.toString(), coop.getBestPlan().getStopsToBeServed().get(2).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);		
		
		
		// generate 100 trips from 2111 to 4443
		Set<Id> personIds = new TreeSet<Id>();		
		
		for (int i = 0; i < 100; i++) {
			Id personId = new IdImpl(i);
			personIds.add(personId);
			strat.handleEvent(new PersonEntersVehicleEvent(18000.0, personId, veh1));
		}
		
		Assert.assertEquals("Number of test persons", 100, personIds.size(), MatsimTestUtils.EPSILON);
		
		strat.handleEvent(new VehicleArrivesAtFacilityEvent(18050.0, veh1, stopId2, 0.0));
		strat.handleEvent(new VehicleArrivesAtFacilityEvent(18100.0, veh1, stopId3, 0.0));
		
		for (Id personId : personIds) {
			strat.handleEvent(new PersonLeavesVehicleEvent(18100.0, personId, veh1));
		}
				
		
		// should be able to replan
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle moved from best plan", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Start stop", stopId1.toString(), testPlan.getStopsToBeServed().get(0).getId().toString());
		Assert.assertEquals("End stop", stopId3.toString(), testPlan.getStopsToBeServed().get(1).getId().toString());
		Assert.assertEquals("Number of stops to be served", 2, testPlan.getStopsToBeServed().size());		
	}
}