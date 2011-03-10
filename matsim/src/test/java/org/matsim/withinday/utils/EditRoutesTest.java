/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
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

package org.matsim.withinday.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PlanImplTest;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculatorFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.testcases.MatsimTestCase;

public class EditRoutesTest extends MatsimTestCase {
	
	static private final Logger log = Logger.getLogger(EditRoutesTest.class);
	
	
	private Scenario scenario;
	private Plan plan;
	private PlanAlgorithm planAlgorithm;
	
	/**
	 * @author cdobler
	 */
	public void testReplanFutureLegRoute() {
		createScenario();
		EditRoutes ed = new EditRoutes();
		
		ActivityImpl activityH1 = (ActivityImpl) plan.getPlanElements().get(0);
		Leg legHW = (Leg) plan.getPlanElements().get(1);
		ActivityImpl activityW1 = (ActivityImpl) plan.getPlanElements().get(2);
		Leg legWH = (Leg) plan.getPlanElements().get(3);
		ActivityImpl activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		
		// expect EditRoutes to return false if the Plan or the PlanAlgorithm is null
		assertEquals(ed.replanFutureLegRoute(null, 1, planAlgorithm), false);
		assertEquals(ed.replanFutureLegRoute(plan, 1, null), false);
		
		// expect EditRoutes to return false if the index does not point to a Leg
		assertEquals(ed.replanFutureLegRoute(plan, 0, planAlgorithm), false);
		assertEquals(ed.replanFutureLegRoute(plan, 2, planAlgorithm), false);
		assertEquals(ed.replanFutureLegRoute(plan, 4, planAlgorithm), false);
		
		// create new, empty routes and set them in the Legs
		NetworkRoute networkRouteHW = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(legHW.getRoute().getStartLinkId(), legHW.getRoute().getEndLinkId());
		NetworkRoute networkRouteWH = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(legWH.getRoute().getStartLinkId(), legWH.getRoute().getEndLinkId());
		legHW.setRoute(networkRouteHW);
		legWH.setRoute(networkRouteWH);
		
		// the routes are empty, therefore the list of Links should be empty
		assertEquals(networkRouteHW.getLinkIds().size(), 0);
		assertEquals(networkRouteWH.getLinkIds().size(), 0);
		
		// replan the legs to recreate the routes
		assertEquals(ed.replanFutureLegRoute(plan, 1, planAlgorithm), true);
		assertEquals(ed.replanFutureLegRoute(plan, 3, planAlgorithm), true);
		
		// the routes have been recreated
		assertEquals(networkRouteHW.getLinkIds().size(), 1);	// l5
		assertEquals(networkRouteWH.getLinkIds().size(), 3);	// l4, l5, l2
		
		
		// move the location of the activities - check whether the start and end Links of the routes are updated
		activityH1.setLinkId(scenario.createId("l4"));
		activityW1.setLinkId(scenario.createId("l2"));
		activityH2.setLinkId(scenario.createId("l4"));
		
		// replan the legs to recreate the routes
		assertEquals(ed.replanFutureLegRoute(plan, 1, planAlgorithm), true);
		assertEquals(ed.replanFutureLegRoute(plan, 3, planAlgorithm), true);
		
		// check the length of the routes
		assertEquals(networkRouteHW.getLinkIds().size(), 1);	// l5
		assertEquals(networkRouteWH.getLinkIds().size(), 3);	// l1, l5, l3
		
		// check whether the start and end Links have been updated
		assertEquals(networkRouteHW.getStartLinkId(), scenario.createId("l4"));
		assertEquals(networkRouteHW.getEndLinkId(), scenario.createId("l2"));
		assertEquals(networkRouteWH.getStartLinkId(), scenario.createId("l2"));
		assertEquals(networkRouteWH.getEndLinkId(), scenario.createId("l4"));
				
		// expect EditRoutes to return false if the Route in the leg is not a NetworkRoute
		Route genericRoute = new GenericRouteFactory().createRoute(new IdImpl("dummy"), new IdImpl("dummy"));
		legHW.setRoute(genericRoute);
		legWH.setRoute(genericRoute);
		assertEquals(ed.replanFutureLegRoute(plan, 1, planAlgorithm), false);
		assertEquals(ed.replanFutureLegRoute(plan, 3, planAlgorithm), false);
	}
	
	/**
	 * @author cdobler
	 */
	public void testReplanCurrentLegRoute() {
		createScenario();
		EditRoutes ed = new EditRoutes();
		
		ActivityImpl activityW1 = null;
		ActivityImpl activityH2 = null;
		
		// expect EditRoutes to return false if the Plan or the PlanAlgorithm is null
		assertEquals(ed.replanCurrentLegRoute(null, 1, 0, planAlgorithm, 8.0*3600), false);
		assertEquals(ed.replanCurrentLegRoute(plan, 1, 0, null, 8.0*3600), false);
		
		// expect EditRoutes to return false if the index does not point to a Leg
		assertEquals(ed.replanCurrentLegRoute(plan, 0, 0, planAlgorithm, 8.0*3600), false);
		assertEquals(ed.replanCurrentLegRoute(plan, 2, 0, planAlgorithm, 8.0*3600), false);
		assertEquals(ed.replanCurrentLegRoute(plan, 4, 0, planAlgorithm, 8.0*3600), false);
		
		// expect ArrayIndexOutOfBoundsException - using illegal indices for the current position in the route
		try {
			ed.replanCurrentLegRoute(plan, 1, -1, planAlgorithm, 8.0*3600);
			ed.replanCurrentLegRoute(plan, 1, 100, planAlgorithm, 8.0*3600);
			fail("expected ArrayIndexOutOfBoundsException.");
		} catch (ArrayIndexOutOfBoundsException e) {
			log.debug("catched expected exception.", e);
		}
		
		// create new routes for HW-trip
		assertEquals(ed.replanCurrentLegRoute(plan, 1, 0, planAlgorithm, 8.0*3600), true);	// HW, start Link
		assertEquals(ed.replanCurrentLegRoute(plan, 1, 1, planAlgorithm, 8.0*3600), true);	// HW, en-route
		assertEquals(ed.replanCurrentLegRoute(plan, 1, 2, planAlgorithm, 8.0*3600), true);	// HW, end Link

		// create new routes for WH-trip
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 0, planAlgorithm, 8.0*3600), true);	// WH, start Link
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 1, planAlgorithm, 8.0*3600), true);	// WH, en-route
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 2, planAlgorithm, 8.0*3600), true);	// WH, en-route
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 3, planAlgorithm, 8.0*3600), true);	// WH, en-route
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 4, planAlgorithm, 8.0*3600), true);	// WH, end Link
		
		/*
		 *  replace destinations and create new routes
		 */
		// create new routes for HW-trip
		createScenario();	// reset scenario
		activityW1 = (ActivityImpl) plan.getPlanElements().get(2);
		activityW1.setLinkId(scenario.createId("l2"));	// move Activity location		
		assertEquals(ed.replanCurrentLegRoute(plan, 1, 0, planAlgorithm, 8.0*3600), true);	// HW, start Link
		assertEquals(checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(1)).getRoute()), true);
		
		createScenario();	// reset scenario
		activityW1 = (ActivityImpl) plan.getPlanElements().get(2);
		activityW1.setLinkId(scenario.createId("l2"));	// move Activity location
		assertEquals(ed.replanCurrentLegRoute(plan, 1, 1, planAlgorithm, 8.0*3600), true);	// HW, en-route
		assertEquals(checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(1)).getRoute()), true);
		
		createScenario();	// reset scenario
		activityW1 = (ActivityImpl) plan.getPlanElements().get(2);
		activityW1.setLinkId(scenario.createId("l2"));	// move Activity location
		assertEquals(ed.replanCurrentLegRoute(plan, 1, 2, planAlgorithm, 8.0*3600), true);	// HW, end Link
		assertEquals(checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(1)).getRoute()), true);
		
		// create new routes for WH-trip
		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(scenario.createId("l4"));	// move Activity location
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 0, planAlgorithm, 8.0*3600), true);	// WH, start Link
		assertEquals(checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(3)).getRoute()), true);
		
		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(scenario.createId("l4"));	// move Activity location
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 1, planAlgorithm, 8.0*3600), true);	// WH, en-route
		assertEquals(checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(3)).getRoute()), true);
		
		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(scenario.createId("l4"));	// move Activity location
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 2, planAlgorithm, 8.0*3600), true);	// WH, en-route
		assertEquals(checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(3)).getRoute()), true);
		
		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(scenario.createId("l4"));	// move Activity location
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 3, planAlgorithm, 8.0*3600), true);	// WH, en-route
		assertEquals(checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(3)).getRoute()), true);

		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(scenario.createId("l4"));	// move Activity location
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 4, planAlgorithm, 8.0*3600), true);	// WH, end Link
		assertEquals(checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(3)).getRoute()), true);
		
		
		// expect EditRoutes to return false if the Route in the leg is not a NetworkRoute
		createScenario();	// reset scenario
		Route genericRoute = new GenericRouteFactory().createRoute(new IdImpl("dummy"), new IdImpl("dummy"));
		Leg legHW = (Leg) plan.getPlanElements().get(1);
		Leg legWH = (Leg) plan.getPlanElements().get(3);
		legHW.setRoute(genericRoute);
		legWH.setRoute(genericRoute);
		assertEquals(ed.replanCurrentLegRoute(plan, 1, 0, planAlgorithm, 8.0*3600), false);
		assertEquals(ed.replanCurrentLegRoute(plan, 3, 0, planAlgorithm, 8.0*3600), false);
	}
	
	/**
	 * @author cdobler
	 */
	private void createScenario() {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		createSampleNetwork();
		createPlanAlgorithm();
		createSamplePlan();
	}
	
	/**
	 * @author cdobler
	 */
	private void createSampleNetwork() {
		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();
		
		Id id = null;
		Coord coord = null;
		Node node1 = null;
		Node node2 = null;
		Node node3 = null;
		Node node4 = null;
		Link link = null;
		
		/*
		 * create Nodes
		 */
		id = scenario.createId("n1");
		coord = scenario.createCoord(0, 0);
		node1 = networkFactory.createNode(id, coord);
		network.addNode(node1);
		
		id = scenario.createId("n2");
		coord = scenario.createCoord(1, 0);
		node2 = networkFactory.createNode(id, coord);
		network.addNode(node2);

		id = scenario.createId("n3");
		coord = scenario.createCoord(1, 1);
		node3 = networkFactory.createNode(id, coord);
		network.addNode(node3);

		id = scenario.createId("n4");
		coord = scenario.createCoord(0, 1);
		node4 = networkFactory.createNode(id, coord);
		network.addNode(node4);

		/*
		 * create Links
		 */
		id = scenario.createId("l1");
		link = networkFactory.createLink(id, node2, node1);
		network.addLink(link);
		
		id = scenario.createId("l2");
		link = networkFactory.createLink(id, node3, node2);
		network.addLink(link);
		
		id = scenario.createId("l3");
		link = networkFactory.createLink(id, node3, node4);
		network.addLink(link);
		
		id = scenario.createId("l4");
		link = networkFactory.createLink(id, node4, node1);
		network.addLink(link);
		
		id = scenario.createId("l5");
		link = networkFactory.createLink(id, node1, node3);
		network.addLink(link);
	}
	
	/**
	 * @author cdobler
	 */
	private void createSamplePlan() {
		plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		
		Activity activityH1 = ((PlanImpl) plan).createAndAddActivity("h", scenario.createId("l1"));
		((PlanImpl) plan).createAndAddLeg(TransportMode.car);
		Activity activityW1 = ((PlanImpl) plan).createAndAddActivity("w", scenario.createId("l3"));
		((PlanImpl) plan).createAndAddLeg(TransportMode.car);
		Activity activityH2 = ((PlanImpl) plan).createAndAddActivity("h", scenario.createId("l1"));
		
		/*
		 * set activity start times and durations
		 */
		activityH1.setStartTime(0.0);
		activityH1.setEndTime(8.0*3600);
		activityW1.setStartTime(8.0*3600);
		activityW1.setEndTime(16.0*3600);		
		activityH2.setStartTime(16.0*3600);
		activityH2.setEndTime(24.0*3600);
		
		/*
		 * run the planAlgorithm to create and set routes
		 */		
		planAlgorithm.run(plan);
	}
	
	/**
	 * @author cdobler
	 */
	private void createPlanAlgorithm() {
		PersonalizableTravelTime travelTime = new FreeSpeedTravelTimeCalculatorFactory().createFreeSpeedTravelTimeCalculator();
		PersonalizableTravelCost travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
		planAlgorithm = new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), 
				travelCost, travelTime, new DijkstraFactory());
	}
	
	/**
	 * @author cdobler
	 */
	private boolean checkRouteValidity(NetworkRoute route) {
		List<Id> linkIds = new ArrayList<Id>();
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		
		// if the route ends on the start link and is not a round trip
		if (route.getStartLinkId() == route.getEndLinkId()) {
			if(linkIds.size() == 2) return true;
		}
		
		int index = 0;
		Link link = null;
		Link nextLink = null;
		
		while (index < linkIds.size() - 1) {
			link = scenario.getNetwork().getLinks().get(linkIds.get(index));
			if (link == null) return false;
			
			index++;
			nextLink = scenario.getNetwork().getLinks().get(linkIds.get(index));
			
			// if the link does not lead to the nextLink
			if (!link.getToNode().equals(nextLink.getFromNode())) return false;
		}
		
		return true;
	}
}