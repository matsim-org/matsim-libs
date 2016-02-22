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
import java.util.Arrays;
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.testcases.MatsimTestCase;

public class EditRoutesTest extends MatsimTestCase {
	// yyyy This test relies heavily on position counting in plans.  With the introduction of intermediate walk legs the positions changed.
	// I tried to guess the correct indices, but it would be more honest to change this to TripStructureUtils.  kai, feb'16
	// h--wlk--iact--car--iact--w  ==> car = 3
	// h--wlk--iact--car--iact--wlk--work--wlk--iact--car--iact--wlk--home  ==> car = 9
	
	
	static private final Logger log = Logger.getLogger(EditRoutesTest.class);
		
	private Scenario scenario;
	private Plan plan;
	private TripRouter tripRouter;
	private LeastCostPathCalculator pathCalculator ;
	private ModeRouteFactory routeFactory ;
	
	/**
	 * @author cdobler
	 */
	public void testReplanFutureLegRoute() {
		// this is ok (we can still replan a single leg with the computer science router). kai, dec'15
		
		createScenario();
		
		
		int firstCarLeg = 1 ; // 1-->3	
		int scndCarLeg = 3 ; // 3-->9
		if ( scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) { 
			firstCarLeg = 3 ; // 1-->3	
			scndCarLeg = 9 ; // 3-->9
		}


		EditRoutes ed = new EditRoutes(scenario.getNetwork(), pathCalculator, routeFactory);
		
		Leg legHW = (Leg) plan.getPlanElements().get(firstCarLeg);
		Leg legWH = (Leg) plan.getPlanElements().get(scndCarLeg);
		
		// create new, empty routes and set them in the Legs
		NetworkRoute networkRouteHW = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(
				legHW.getRoute().getStartLinkId(), legHW.getRoute().getEndLinkId());
		NetworkRoute networkRouteWH = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(
				legWH.getRoute().getStartLinkId(), legWH.getRoute().getEndLinkId());
		legHW.setRoute(networkRouteHW);
		legWH.setRoute(networkRouteWH);
		
		// the routes are empty, therefore the list of Links should be empty
		assertEquals(networkRouteHW.getLinkIds().size(), 0);
		assertEquals(networkRouteWH.getLinkIds().size(), 0);
		
		// replan the legs to recreate the routes

//		assertEquals(true, ed.replanFutureLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), scenario.getNetwork(), tripRouter));
		assertEquals(true, ed.replanFutureLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson() ) ); // 1-->3

//		assertEquals(true, ed.replanFutureLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), scenario.getNetwork(), tripRouter));
		assertEquals(true, ed.replanFutureLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson() ) ); // 3-->9
		
		// the legs have been replaced, the original ones should not have changed
		assertEquals(0, networkRouteHW.getLinkIds().size());
		assertEquals(0, networkRouteWH.getLinkIds().size());
		
		// get replaced legs and routes
		legHW = (Leg) plan.getPlanElements().get(firstCarLeg); // 1-->3
		legWH = (Leg) plan.getPlanElements().get(scndCarLeg); // 3-->9
		networkRouteHW = (NetworkRoute) legHW.getRoute();
		networkRouteWH = (NetworkRoute) legWH.getRoute();
		
		System.err.println( "hw:" + networkRouteHW );
		System.err.println( "wh:" + networkRouteWH );
		
		// the routes have been recreated
		assertEquals(1, networkRouteHW.getLinkIds().size());	// l5
		assertEquals(3, networkRouteWH.getLinkIds().size());	// l4, l5, l2
	}
	
	public void testRelocateFutureLegRoute() {
		// yyyy this test is misleading.  "relocateFutureLegRoute"  is ok, but it does not look after the overall plan consistency, 
		// as this test implies.  kai, feb'16
		

		createScenario();

		int firstCarLeg = 1 ; // 1-->3	
		int scndAct = 2 ;
		int scndCarLeg = 3 ; // 3-->9
		int thrdAct = 4 ;
		if ( scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) { 
			firstCarLeg = 3 ; // 1-->3
			scndAct = 6 ;
			scndCarLeg = 9 ; // 3-->9
			thrdAct = 12 ;
		}

		EditRoutes ed = new EditRoutes(scenario.getNetwork(), pathCalculator, routeFactory);
		
		Activity activityH1 = (Activity) plan.getPlanElements().get(0);
		Leg legHW = (Leg) plan.getPlanElements().get(firstCarLeg);
		Activity activityW1 = (Activity) plan.getPlanElements().get(scndAct);
		Leg legWH = (Leg) plan.getPlanElements().get(scndCarLeg);
		Activity activityH2 = (Activity) plan.getPlanElements().get(thrdAct);
		
		// create new, empty routes and set them in the Legs
		NetworkRoute networkRouteHW = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(
				legHW.getRoute().getStartLinkId(), legHW.getRoute().getEndLinkId());
		NetworkRoute networkRouteWH = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(
				legWH.getRoute().getStartLinkId(), legWH.getRoute().getEndLinkId());
		legHW.setRoute(networkRouteHW);
		legWH.setRoute(networkRouteWH);
		
		// move the location of the activities - check whether the start and end Links of the routes are updated
		activityH1.setLinkId(Id.create("l4", Link.class));
		activityW1.setLinkId(Id.create("l2", Link.class));
		activityH2.setLinkId(Id.create("l4", Link.class));
		
		// relocate the legs to recreate the routes
//		assertEquals(true, ed.relocateFutureLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), activityH1.getLinkId(), activityW1.getLinkId(), plan.getPerson(), scenario.getNetwork(), tripRouter));
		assertEquals(true, ed.relocateFutureLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), activityH1.getLinkId(), activityW1.getLinkId(), plan.getPerson() ));
		// 1-->3
		
//		assertEquals(true, ed.relocateFutureLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), activityW1.getLinkId(), activityH2.getLinkId(), plan.getPerson(), scenario.getNetwork(), tripRouter));
		assertEquals(true, ed.relocateFutureLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), activityW1.getLinkId(), activityH2.getLinkId(), plan.getPerson() ));
		// 3-->9
		
		// get replaced legs and routes
		legHW = (Leg) plan.getPlanElements().get(firstCarLeg); // 1-->3
		legWH = (Leg) plan.getPlanElements().get(scndCarLeg); // 3-->9
		networkRouteHW = (NetworkRoute) legHW.getRoute();
		networkRouteWH = (NetworkRoute) legWH.getRoute();
		
		// check the length of the routes
		assertEquals(1, networkRouteHW.getLinkIds().size());	// l5
		assertEquals(3, networkRouteWH.getLinkIds().size());	// l1, l5, l3
		
		// check whether the start and end Links have been updated
		assertEquals(Id.create("l4", Link.class), networkRouteHW.getStartLinkId());
		assertEquals(Id.create("l2", Link.class), networkRouteHW.getEndLinkId());
		assertEquals(Id.create("l2", Link.class), networkRouteWH.getStartLinkId());
		assertEquals(Id.create("l4", Link.class), networkRouteWH.getEndLinkId());
		
		// check whether non-car routes are also generated
		legHW.setRoute(null);
		legHW.setMode(TransportMode.walk);
		legWH.setRoute(null);
		legWH.setMode(TransportMode.walk);
		assertEquals(true, ed.relocateFutureLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), activityH1.getLinkId(), activityW1.getLinkId(), plan.getPerson() ));
		// 1-->3
		
		assertEquals(true, ed.relocateFutureLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), activityW1.getLinkId(), activityH2.getLinkId(), plan.getPerson() ));
		// 3-->9
		
		assertNotNull(legHW.getRoute());
		assertNotNull(legWH.getRoute());
	}
	
	/**
	 * @author cdobler
	 */
	public void testReplanCurrentLegRoute() {
		// this is ok (we can still replan a single leg with the computer science router). kai, dec'15
		
		createScenario();
		
		int firstCarLeg = 1 ; // 1-->3	
		int scndCarLeg = 3 ; // 3-->9
		if ( scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) { 
			firstCarLeg = 3 ; // 1-->3	
			scndCarLeg = 9 ; // 3-->9
		}


		
		EditRoutes ed = new EditRoutes(scenario.getNetwork(), pathCalculator, routeFactory);
		
		ActivityImpl activityW1 = null;
		ActivityImpl activityH2 = null;
		
//		// expect EditRoutes to return false if the index does not point to a Leg
//		assertEquals(false, ed.replanCurrentLegRoute(plan, 0, 0, tripRouter, 8.0*3600));
//		assertEquals(false, ed.replanCurrentLegRoute(plan, 2, 0, tripRouter, 8.0*3600));
//		assertEquals(false, ed.replanCurrentLegRoute(plan, 4, 0, tripRouter, 8.0*3600));
		
		// expect ArrayIndexOutOfBoundsException - using illegal indices for the current position in the route
		try {
				ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), -1, 8.0*3600 );  
				ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), 100, 8.0*3600 );
				fail("expected ArrayIndexOutOfBoundsException.");
		} catch (ArrayIndexOutOfBoundsException e) {
			log.debug("catched expected exception.", e);
		}
		
			// create new routes for HW-trip
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), 0, 8.0*3600 )); // HW, start Link
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), 1, 8.0*3600 )); // HW, en-route
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), 2, 8.0*3600 )); // HW, end Link

			// create new routes for WH-trip
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 0, 8.0*3600 )); // WH, start Link
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 1, 8.0*3600 )); // WH, en-route
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 2, 8.0*3600 )); // WH, en-route
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 3, 8.0*3600 )); // WH, en-route
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 4, 8.0*3600 )); // WH, end Link

		/*
		 *  replace destinations and create new routes
		 */
		// create new routes for HW-trip
		createScenario();	// reset scenario
		activityW1 = (ActivityImpl) plan.getPlanElements().get(2);
		activityW1.setLinkId(Id.create("l2", Link.class));	// move Activity location		
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), 0, 8.0*3600 )); // HW, start Link

		final NetworkRoute route = (NetworkRoute)((Leg)plan.getPlanElements().get(firstCarLeg)).getRoute();
		log.warn( route );
		assertEquals(true, checkRouteValidity(route));
		
		createScenario();	// reset scenario
		activityW1 = (ActivityImpl) plan.getPlanElements().get(2);
		activityW1.setLinkId(Id.create("l2", Link.class));	// move Activity location
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), 1, 8.0*3600 ) );	// HW, en-route			
			assertEquals(true, checkRouteValidity(route));
		
		createScenario();	// reset scenario
		activityW1 = (ActivityImpl) plan.getPlanElements().get(2);
		activityW1.setLinkId(Id.create("l2", Link.class));	// move Activity location
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), 2, 8.0*3600 ));	// HW, end Link			
		assertEquals(true, checkRouteValidity(route));
		
		// create new routes for WH-trip
		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(Id.create("l4", Link.class));	// move Activity location
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 0, 8.0*3600 ));	// WH, start Link			
		assertEquals(true, checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(scndCarLeg)).getRoute()));
		
		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(Id.create("l4", Link.class));	// move Activity location
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 1, 8.0*3600 ));	// WH, en-route
		assertEquals(true, checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(scndCarLeg)).getRoute()));
		
		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(Id.create("l4", Link.class));	// move Activity location
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 2, 8.0*3600 ));	// WH, en-route			
		assertEquals(true, checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(scndCarLeg)).getRoute()));
		
		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(Id.create("l4", Link.class));	// move Activity location
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 3, 8.0*3600 ) );	// WH, en-route			
		assertEquals(true, checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(scndCarLeg)).getRoute()));

		createScenario();	// reset scenario
		activityH2 = (ActivityImpl) plan.getPlanElements().get(4);
		activityH2.setLinkId(Id.create("l4", Link.class));	// move Activity location
			assertEquals(true, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 4, 8.0*3600 ));	// WH, end Link			
		assertEquals(true, checkRouteValidity((NetworkRoute)((Leg)plan.getPlanElements().get(scndCarLeg)).getRoute()));
		
		// expect EditRoutes to return false if the Route in the leg is not a NetworkRoute
		createScenario();	// reset scenario
		Leg legHW = (Leg) plan.getPlanElements().get(firstCarLeg);
		Leg legWH = (Leg) plan.getPlanElements().get(scndCarLeg);
		legHW.setRoute(null);
		legHW.setMode(TransportMode.walk);
		legWH.setRoute(null);
		legWH.setMode(TransportMode.walk);
			assertEquals(false, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(firstCarLeg), plan.getPerson(), 0, 8.0*3600 ));
			assertEquals(false, ed.replanCurrentLegRoute((Leg) plan.getPlanElements().get(scndCarLeg), plan.getPerson(), 0, 8.0*3600 ));
	}
	
	/**
	 * @author cdobler
	 */
	private void createScenario() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		createSampleNetwork();
		createTripRouter();
		createSamplePlan();
	}
	
	/**
	 * @author cdobler
	 */
	private void createSampleNetwork() {
		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();
		
		Coord coord = null;
		Node node1 = null;
		Node node2 = null;
		Node node3 = null;
		Node node4 = null;
		Link link = null;
		
		/*
		 * create Nodes
		 */
		coord = new Coord((double) 0, (double) 0);
		node1 = networkFactory.createNode(Id.create("n1", Node.class), coord);
		network.addNode(node1);

		coord = new Coord((double) 1, (double) 0);
		node2 = networkFactory.createNode(Id.create("n2", Node.class), coord);
		network.addNode(node2);

		coord = new Coord((double) 1, (double) 1);
		node3 = networkFactory.createNode(Id.create("n3", Node.class), coord);
		network.addNode(node3);

		coord = new Coord((double) 0, (double) 1);
		node4 = networkFactory.createNode(Id.create("n4", Node.class), coord);
		network.addNode(node4);

		/*
		 * create Links
		 */
		link = networkFactory.createLink(Id.create("l1", Link.class), node2, node1);
		network.addLink(link);
		
		link = networkFactory.createLink(Id.create("l2", Link.class), node3, node2);
		network.addLink(link);
		
		link = networkFactory.createLink(Id.create("l3", Link.class), node3, node4);
		network.addLink(link);
		
		link = networkFactory.createLink(Id.create("l4", Link.class), node4, node1);
		network.addLink(link);
		
		link = networkFactory.createLink(Id.create("l5", Link.class), node1, node3);
		network.addLink(link);
	}
	
	/**
	 * @author cdobler
	 */
	private void createSamplePlan() {
		plan = new PlanImpl(PopulationUtils.createPerson(Id.create(1, Person.class)));
		
		Activity activityH1 = ((PlanImpl) plan).createAndAddActivity("h", Id.create("l1", Link.class));
		((PlanImpl) plan).createAndAddLeg(TransportMode.car);
		Activity activityW1 = ((PlanImpl) plan).createAndAddActivity("w", Id.create("l3", Link.class));
		((PlanImpl) plan).createAndAddLeg(TransportMode.car);
		Activity activityH2 = ((PlanImpl) plan).createAndAddActivity("h", Id.create("l1", Link.class));
		
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
		 * set coordinates for activities
		 */
		((ActivityImpl) activityH1).setCoord(scenario.getNetwork().getLinks().get(activityH1.getLinkId()).getCoord());
		((ActivityImpl) activityW1).setCoord(scenario.getNetwork().getLinks().get(activityW1.getLinkId()).getCoord());
		((ActivityImpl) activityH2).setCoord(scenario.getNetwork().getLinks().get(activityH2.getLinkId()).getCoord());
		
		/*
		 * run a PlanRouter to create and set routes
		 */
		new PlanRouter(tripRouter).run(plan);
	}
	
	/**
	 * @author cdobler
	 */
	private void createTripRouter() {
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(AbstractModule.override(Arrays.asList(new TripRouterModule()), new AbstractModule() {
					@Override
					public void install() {
						install(new ScenarioByInstanceModule(scenario));
						addTravelTimeBinding("car").toInstance(new FreeSpeedTravelTime());
						addTravelDisutilityFactoryBinding("car").toInstance(new OnlyTimeDependentTravelDisutilityFactory());
					}
				}));
			}
		});
		tripRouter = injector.getInstance(TripRouter.class);
		
		TravelTime timeFunction = new FreeSpeedTravelTime() ;
		TravelDisutility costFunction = new OnlyTimeDependentTravelDisutility( timeFunction ) ;
		this.pathCalculator = new Dijkstra(scenario.getNetwork(), costFunction, timeFunction) ;
		
		this.routeFactory = ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getModeRouteFactory() ;
	}
	
	/**
	 * @author cdobler
	 */
	private boolean checkRouteValidity(NetworkRoute route) {
		List<Id<Link>> linkIds = new ArrayList<>();
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