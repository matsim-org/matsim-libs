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

package org.matsim.contrib.minibus.routeProvider;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.routeProvider.ComplexCircleScheduleProvider;
import org.matsim.contrib.minibus.routeProvider.RandomStopProvider;
import org.matsim.contrib.minibus.routeProvider.SimpleCircleScheduleProvider;
import org.matsim.contrib.minibus.schedule.CreateStopsForAllCarLinks;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

public class ComplexCircleScheduleProviderTest {
	
@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testCreateTransitLineLikeSimpleCircleScheduleProvider() {
		
		Scenario scenario = PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();
		
		
		TransitSchedule tS = CreateStopsForAllCarLinks.createStopsForAllCarLinks(scenario.getNetwork(), pC);
		
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(tS, scenario.getNetwork(), null, pC.getVehicleMaximumVelocity(), pC.getPlanningSpeedFactor(), pC.getDriverRestTime(), pC.getMode());
		
		Id<Operator> lineId = Id.create("line1", Operator.class);
		Id<PPlan> routeId = Id.create("route1", PPlan.class);
		
		PPlan plan = new PPlan(routeId, "noCreator", PConstants.founderPlanId);
		plan.setStartTime(7.0 * 3600.0);
		plan.setEndTime(9.0 * 3600.0);
		plan.setNVehicles(2);
		TransitStopFacility startStop = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "1424", org.matsim.facilities.Facility.class));
		TransitStopFacility endStop = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "4434", org.matsim.facilities.Facility.class));
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		stopsToBeServed.add(startStop);
		stopsToBeServed.add(endStop);
		plan.setStopsToBeServed(stopsToBeServed);
		
		ArrayList<Id<Link>> refIds = new ArrayList<>();
		refIds.add(Id.create("1424", Link.class)); refIds.add(Id.create("2434", Link.class));
		refIds.add(Id.create("3444", Link.class)); refIds.add(Id.create("4434", Link.class));
		refIds.add(Id.create("3424", Link.class)); refIds.add(Id.create("2414", Link.class));
		refIds.add(Id.create("1424", Link.class));
		
		TransitLine line = prov.createTransitLineFromOperatorPlan(lineId, plan);
		
		Assert.assertEquals("Transit line ids have to be the same", Id.create(lineId, TransitLine.class), line.getId());
		
		for (TransitRoute route : line.getRoutes().values()) {
			Assert.assertEquals("Route id have to be the same", Id.create(lineId + "-" + routeId, TransitRoute.class), route.getId());
			Assert.assertEquals("Number of departures", 14.0, route.getDepartures().size(), MatsimTestUtils.EPSILON);
			
			// check links			
			Assert.assertEquals("Start link id has to be the same", refIds.get(0), route.getRoute().getStartLinkId());
			
			int i = 1;
			for (Id<Link> linkId : route.getRoute().getLinkIds()) {
				Assert.assertEquals("Route link ids have to be the same", refIds.get(i), linkId);
				i++;
			}
			
			Assert.assertEquals("End link id has to be the same", refIds.get(refIds.size() - 1), route.getRoute().getEndLinkId());
			
			// check stops
			i = 0;
			for (TransitRouteStop stop : route.getStops()) {
				Assert.assertEquals("Route stop ids have to be the same", Id.create(pC.getPIdentifier() + refIds.get(i), org.matsim.facilities.Facility.class), stop.getStopFacility().getId());
				i++;
			}			
		}	
	}
	
	@Test
    public final void testCreateTransitLineWithMoreStops() {
		
		Scenario scenario = PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();
		
		
		TransitSchedule tS = CreateStopsForAllCarLinks.createStopsForAllCarLinks(scenario.getNetwork(), pC);
		
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(tS, scenario.getNetwork(), null, pC.getVehicleMaximumVelocity(), pC.getPlanningSpeedFactor(), pC.getDriverRestTime(), pC.getMode());
		
		Id<Operator> lineId = Id.create("line1", Operator.class);
		Id<PPlan> routeId = Id.create("route1", PPlan.class);
		
		PPlan plan = new PPlan(routeId, "noCreator",PConstants.founderPlanId);
		plan.setStartTime(7.0 * 3600.0);
		plan.setEndTime(9.0 * 3600.0);
		plan.setNVehicles(2);
		TransitStopFacility stop1 = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "1424", org.matsim.facilities.Facility.class));
		TransitStopFacility stop2 = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "2423", org.matsim.facilities.Facility.class));
		TransitStopFacility stop3 = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "2333", org.matsim.facilities.Facility.class));
		TransitStopFacility stop4 = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "3433", org.matsim.facilities.Facility.class));
		TransitStopFacility stop5 = tS.getFacilities().get(Id.create(pC.getPIdentifier() + "3334", org.matsim.facilities.Facility.class));
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		stopsToBeServed.add(stop1);
		stopsToBeServed.add(stop2);
		stopsToBeServed.add(stop3);
		stopsToBeServed.add(stop4);
		stopsToBeServed.add(stop5);
		
		plan.setStopsToBeServed(stopsToBeServed);
		
		ArrayList<Id<Link>> refIds = new ArrayList<>();
		refIds.add(Id.create("1424", Link.class)); refIds.add(Id.create("2423", Link.class));
		refIds.add(Id.create("2333", Link.class)); refIds.add(Id.create("3334", Link.class));
		refIds.add(Id.create("3433", Link.class)); refIds.add(Id.create("3334", Link.class));
		refIds.add(Id.create("3424", Link.class)); refIds.add(Id.create("2414", Link.class));
		refIds.add(Id.create("1424", Link.class));
		
		TransitLine line = prov.createTransitLineFromOperatorPlan(lineId, plan);
		
		Assert.assertEquals("Transit line ids have to be the same", Id.create(lineId, TransitLine.class), line.getId());
		
		for (TransitRoute route : line.getRoutes().values()) {
			Assert.assertEquals("Route id have to be the same", Id.create(lineId + "-" + routeId, TransitRoute.class), route.getId());
			
			// check links			
			Assert.assertEquals("Start link id has to be the same", refIds.get(0), route.getRoute().getStartLinkId());
			
			int i = 1;
			for (Id<Link> linkId : route.getRoute().getLinkIds()) {
				Assert.assertEquals("Route link ids have to be the same", refIds.get(i), linkId);
				i++;
			}
			
			Assert.assertEquals("End link id has to be the same", refIds.get(refIds.size() - 1), route.getRoute().getEndLinkId());
			
			// check stops
			i = 0;
			for (TransitRouteStop stop : route.getStops()) {
				Assert.assertEquals("Route stop ids have to be the same", Id.create(pC.getPIdentifier() + refIds.get(i), org.matsim.facilities.Facility.class), stop.getStopFacility().getId());
				i++;
			}
			
			Assert.assertEquals("Number of departures", 11.0, route.getDepartures().size(), MatsimTestUtils.EPSILON);			
		}	
	}
	
	@Test
    public final void testGetRandomTransitStop() {
		
		MutableScenario scenario = (MutableScenario) PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();
		RandomStopProvider randomStopProvider = new RandomStopProvider(pC, scenario.getPopulation(), scenario.getTransitSchedule(), null);
		
		SimpleCircleScheduleProvider prov = new SimpleCircleScheduleProvider(pC.getPIdentifier(), scenario.getTransitSchedule(), scenario.getNetwork(), randomStopProvider, pC.getVehicleMaximumVelocity(), pC.getDriverRestTime(), pC.getMode());
		
		for (int i = 0; i < 5; i++) {
			TransitStopFacility stop1 = prov.getRandomTransitStop(0);
			TransitStopFacility stop2 = prov.getRandomTransitStop(0);
			Assert.assertNotSame("Stop should not be the same", stop1.getId(), stop2.getId());			
		}
	}	
	
	@Test
    public final void testCreateEmptyLine() {
		
		MutableScenario scenario = (MutableScenario) PScenarioHelper.createTestNetwork();
		PConfigGroup pC = new PConfigGroup();
		
		Id<Operator> lineId = Id.create("1", Operator.class);
		
		SimpleCircleScheduleProvider prov = new SimpleCircleScheduleProvider(pC.getPIdentifier(), scenario.getTransitSchedule(), scenario.getNetwork(), null, pC.getVehicleMaximumVelocity(), pC.getDriverRestTime(), pC.getMode());
		TransitLine line = prov.createEmptyLineFromOperator(lineId);
		
		Assert.assertEquals("Transit line ids have to be the same", Id.create(lineId, TransitLine.class), line.getId());
	}
}
