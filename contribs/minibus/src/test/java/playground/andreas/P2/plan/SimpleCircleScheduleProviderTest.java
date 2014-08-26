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

package playground.andreas.P2.plan;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.routeProvider.RandomStopProvider;
import playground.andreas.P2.routeProvider.SimpleCircleScheduleProvider;
import playground.andreas.P2.schedule.CreateStopsForAllCarLinks;

public class SimpleCircleScheduleProviderTest {
	
@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testCreateTransitLine() {
		
		Scenario scenario = PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();
		
		
		TransitSchedule tS = CreateStopsForAllCarLinks.createStopsForAllCarLinks(scenario.getNetwork(), pC);
		
		SimpleCircleScheduleProvider prov = new SimpleCircleScheduleProvider(pC.getPIdentifier(), tS, scenario.getNetwork(), null, 10, pC.getVehicleMaximumVelocity(), pC.getMode());
		
		Id lineId = new IdImpl("line1");
		Id routeId = new IdImpl("route1");
		
		PPlan plan = new PPlan(routeId, "noCreator");
		plan.setStartTime(7.0 * 3600.0);
		plan.setEndTime(9.0 * 3600.0);
		plan.setNVehicles(2);
		TransitStopFacility startStop = tS.getFacilities().get(new IdImpl(pC.getPIdentifier() + "1424"));
		TransitStopFacility endStop = tS.getFacilities().get(new IdImpl(pC.getPIdentifier() + "4434"));
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<TransitStopFacility>();
		stopsToBeServed.add(startStop);
		stopsToBeServed.add(endStop);
		plan.setStopsToBeServed(stopsToBeServed);
		
		ArrayList<Id> refIds = new ArrayList<Id>();
		refIds.add(new IdImpl("1424")); refIds.add(new IdImpl("2434"));
		refIds.add(new IdImpl("3444")); refIds.add(new IdImpl("4434"));
		refIds.add(new IdImpl("3424")); refIds.add(new IdImpl("2414"));
		refIds.add(new IdImpl("1424"));
		
		TransitLine line = prov.createTransitLine(lineId, plan);
		
		Assert.assertEquals("Transit line ids have to be the same", lineId, line.getId());
		
		for (TransitRoute route : line.getRoutes().values()) {
			Assert.assertEquals("Route id have to be the same", new IdImpl(lineId + "-" + routeId), route.getId());
			Assert.assertEquals("Number of departures", 14.0, route.getDepartures().size(), MatsimTestUtils.EPSILON);
			
			// check stops
			int i = 0;
			for (TransitRouteStop stop : route.getStops()) {
				Assert.assertEquals("Route stop ids have to be the same", new IdImpl(pC.getPIdentifier() + refIds.get(i)), stop.getStopFacility().getId());
				i++;
			}
			
			// check links			
			Assert.assertEquals("Start link id has to be the same", refIds.get(0), route.getRoute().getStartLinkId());
			
			i = 1;
			for (Id linkId : route.getRoute().getLinkIds()) {
				Assert.assertEquals("Route link ids have to be the same", refIds.get(i), linkId);
				i++;
			}
			
			Assert.assertEquals("End link id has to be the same", refIds.get(refIds.size() - 1), route.getRoute().getEndLinkId());
		}		
	}
	
	@Test
    public final void testGetRandomTransitStop() {
		
		ScenarioImpl scenario = (ScenarioImpl) PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();
		RandomStopProvider randomStopProvider = new RandomStopProvider(pC, scenario.getPopulation(), scenario.getTransitSchedule(), null);
		
		SimpleCircleScheduleProvider prov = new SimpleCircleScheduleProvider(pC.getPIdentifier(), scenario.getTransitSchedule(), scenario.getNetwork(), randomStopProvider, 10, pC.getVehicleMaximumVelocity(), pC.getMode());
		
		for (int i = 0; i < 5; i++) {
			TransitStopFacility stop1 = prov.getRandomTransitStop(0);
			TransitStopFacility stop2 = prov.getRandomTransitStop(0);
			Assert.assertNotSame("Stop should not be the same", stop1.getId(), stop2.getId());			
		}
	}	
	
	@Test
    public final void testCreateEmptyLine() {
		
		ScenarioImpl scenario = (ScenarioImpl) PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();
		
		Id lineId = new IdImpl("1");
		
		SimpleCircleScheduleProvider prov = new SimpleCircleScheduleProvider(pC.getPIdentifier(), scenario.getTransitSchedule(), scenario.getNetwork(), null, 10, pC.getVehicleMaximumVelocity(), pC.getMode());
		TransitLine line = prov.createEmptyLine(lineId);
		
		Assert.assertEquals("Transit line ids have to be the same", lineId, line.getId());
	}
}