/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalLegHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.BikeTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.PTTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.RideTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.WalkTravelTime;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LegRouter;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author cdobler
 */
public class MultiModalLegRouterTest extends MatsimTestCase {

	public void testRouteLeg() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		
		/*
		 * Create travel time object
		 */
		// pre-initialize the travel time calculator to be able to use it in the wrapper
		TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
		TravelTimeCalculator travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(scenario.getNetwork(), config.travelTimeCalculator());
	
		PlansCalcRouteConfigGroup configGroup = config.plansCalcRoute();
		Map<String, TravelTime> multiModalTravelTimes = new HashMap<String, TravelTime>();
		multiModalTravelTimes.put(TransportMode.car, travelTimeCalculator);
		multiModalTravelTimes.put(TransportMode.walk, new WalkTravelTime(configGroup));
		multiModalTravelTimes.put(TransportMode.bike, new BikeTravelTime(configGroup, new WalkTravelTime(configGroup)));
		multiModalTravelTimes.put(TransportMode.ride, new RideTravelTime(travelTimeCalculator, new WalkTravelTime(configGroup)));
		multiModalTravelTimes.put(TransportMode.pt, new PTTravelTime(configGroup, travelTimeCalculator, new WalkTravelTime(configGroup)));
		
		Map<String, LegRouter> legRouters = createLegRouters(config, scenario.getNetwork(), multiModalTravelTimes);

		Person person = scenario.getPopulation().getFactory().createPerson(scenario.createId("person"));
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		person.addPlan(plan);
		Activity startActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("start", scenario.createId("startLink"));
		Leg leg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		Activity endActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("end", scenario.createId("endLink"));
		
		String transportMode;
		/*
		 * check car mode
		 */
		transportMode = TransportMode.car;
		leg.setMode(transportMode);
		legRouters.get(transportMode).routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.car}), leg, scenario.getNetwork());
		
		/*
		 * check pt mode
		 */
		transportMode = TransportMode.pt;
		leg.setMode(transportMode);
		legRouters.get(transportMode).routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.car, TransportMode.pt}), leg, scenario.getNetwork());
		
		/*
		 * check walk mode
		 */
		transportMode = TransportMode.walk;
		leg.setMode(transportMode);
		legRouters.get(transportMode).routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.walk, TransportMode.bike}), leg, scenario.getNetwork());
		
		/*
		 * check bike mode
		 */
		transportMode = TransportMode.bike;
		leg.setMode(transportMode);
		legRouters.get(transportMode).routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.walk, TransportMode.bike}), leg, scenario.getNetwork());
		
		/*
		 * check ride mode
		 */
		transportMode = TransportMode.ride;
		leg.setMode(transportMode);
		legRouters.get(transportMode).routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.car, TransportMode.walk, TransportMode.bike}), leg, scenario.getNetwork());
	}
	
	private void checkRoute(Set<String> allowedModes, Leg leg, Network network) {
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		
		for (Id id : route.getLinkIds()) {
			Link link = network.getLinks().get(id);
			boolean validMode = false;
			for (String transportMode : link.getAllowedModes()) {
				if (link.getAllowedModes().contains(transportMode)) {
					validMode = true;
					break;
				}
			}
			assertTrue(validMode);
			
		}
	}
	
	private void createNetwork(Scenario scenario) {

		/*
		 * create nodes
		 */
		Node startNode = scenario.getNetwork().getFactory().createNode(scenario.createId("startNode"), scenario.createCoord(0.0, 0.0));
		Node splitNode = scenario.getNetwork().getFactory().createNode(scenario.createId("splitNode"), scenario.createCoord(1.0, 0.0));
		
		Node carNode = scenario.getNetwork().getFactory().createNode(scenario.createId("carNode"), scenario.createCoord(2.0, 2.0));
		Node ptNode = scenario.getNetwork().getFactory().createNode(scenario.createId("ptNode"), scenario.createCoord(2.0, 1.0));
		Node walkNode = scenario.getNetwork().getFactory().createNode(scenario.createId("walkNode"), scenario.createCoord(2.0, 0.0));
		Node bikeNode = scenario.getNetwork().getFactory().createNode(scenario.createId("bikeNode"), scenario.createCoord(2.0, -1.0));
		Node rideNode = scenario.getNetwork().getFactory().createNode(scenario.createId("rideNode"), scenario.createCoord(2.0, -2.0));
		
		Node joinNode = scenario.getNetwork().getFactory().createNode(scenario.createId("joinNode"), scenario.createCoord(1.0, 0.0));
		Node endNode = scenario.getNetwork().getFactory().createNode(scenario.createId("endNode"), scenario.createCoord(0.0, 0.0));
		
		/*
		 * create links
		 */
		Link startLink = scenario.getNetwork().getFactory().createLink(scenario.createId("startLink"), startNode, splitNode);
		
		Link toCarLink = scenario.getNetwork().getFactory().createLink(scenario.createId("toCarLink"), splitNode, carNode);
		Link toPtLink = scenario.getNetwork().getFactory().createLink(scenario.createId("toPtLink"), splitNode, ptNode);
		Link toWalkLink = scenario.getNetwork().getFactory().createLink(scenario.createId("toWalkLink"), splitNode, walkNode);
		Link toBikeLink = scenario.getNetwork().getFactory().createLink(scenario.createId("toBikeLink"), splitNode, bikeNode);
		Link toRideLink = scenario.getNetwork().getFactory().createLink(scenario.createId("toRideLink"), splitNode, rideNode);

		Link fromCarLink = scenario.getNetwork().getFactory().createLink(scenario.createId("fromCarLink"), carNode, joinNode);
		Link fromPtLink = scenario.getNetwork().getFactory().createLink(scenario.createId("fromPtLink"), ptNode, joinNode);
		Link fromWalkLink = scenario.getNetwork().getFactory().createLink(scenario.createId("fromWalkLink"),  walkNode, joinNode);
		Link fromBikeLink = scenario.getNetwork().getFactory().createLink(scenario.createId("fromBikeLink"), bikeNode, joinNode);
		Link fromRideLink = scenario.getNetwork().getFactory().createLink(scenario.createId("fromRideLink"), rideNode, joinNode);
		
		Link endLink = scenario.getNetwork().getFactory().createLink(scenario.createId("endLink"), joinNode, endNode);
		
		/*
		 * set link parameter
		 */
		startLink.setLength(1.0);
		toCarLink.setLength(10.0);
		toPtLink.setLength(10.0);
		toWalkLink.setLength(10.0);
		toBikeLink.setLength(10.0);
		toRideLink.setLength(10.0);
		fromCarLink.setLength(10.0);
		fromPtLink.setLength(10.0);
		fromWalkLink.setLength(10.0);
		fromBikeLink.setLength(10.0);
		fromRideLink.setLength(10.0);
		endLink.setLength(1.0);
		
		startLink.setFreespeed(120.0/3.6);
		toCarLink.setFreespeed(120.0/3.6);
		toPtLink.setFreespeed(120.0/3.6);
		toWalkLink.setFreespeed(120.0/3.6);
		toBikeLink.setFreespeed(120.0/3.6);
		toRideLink.setFreespeed(120.0/3.6);
		fromCarLink.setFreespeed(120.0/3.6);
		fromPtLink.setFreespeed(120.0/3.6);
		fromWalkLink.setFreespeed(120.0/3.6);
		fromBikeLink.setFreespeed(120.0/3.6);
		fromRideLink.setFreespeed(120.0/3.6);
		endLink.setFreespeed(120.0/3.6);

		toCarLink.setAllowedModes(createSet(new String[]{TransportMode.car}));
		toPtLink.setAllowedModes(createSet(new String[]{TransportMode.pt}));
		toWalkLink.setAllowedModes(createSet(new String[]{TransportMode.walk}));
		toBikeLink.setAllowedModes(createSet(new String[]{TransportMode.bike}));
		toRideLink.setAllowedModes(createSet(new String[]{TransportMode.ride}));
		fromCarLink.setAllowedModes(createSet(new String[]{TransportMode.car}));
		fromPtLink.setAllowedModes(createSet(new String[]{TransportMode.pt}));
		fromWalkLink.setAllowedModes(createSet(new String[]{TransportMode.walk}));
		fromBikeLink.setAllowedModes(createSet(new String[]{TransportMode.bike}));
		fromRideLink.setAllowedModes(createSet(new String[]{TransportMode.ride}));
	
		/*
		 * add nodes to network
		 */
		scenario.getNetwork().addNode(startNode);
		scenario.getNetwork().addNode(splitNode);
		scenario.getNetwork().addNode(carNode);
		scenario.getNetwork().addNode(ptNode);
		scenario.getNetwork().addNode(walkNode);
		scenario.getNetwork().addNode(bikeNode);
		scenario.getNetwork().addNode(rideNode);
		scenario.getNetwork().addNode(joinNode);
		scenario.getNetwork().addNode(endNode);
		
		/*
		 * add links to network
		 */
		scenario.getNetwork().addLink(startLink);
		scenario.getNetwork().addLink(toCarLink);
		scenario.getNetwork().addLink(toPtLink);
		scenario.getNetwork().addLink(toWalkLink);
		scenario.getNetwork().addLink(toBikeLink);
		scenario.getNetwork().addLink(toRideLink);
		scenario.getNetwork().addLink(fromCarLink);
		scenario.getNetwork().addLink(fromPtLink);
		scenario.getNetwork().addLink(fromWalkLink);
		scenario.getNetwork().addLink(fromBikeLink);
		scenario.getNetwork().addLink(fromRideLink);
		scenario.getNetwork().addLink(endLink);	
	}
	
	private Set<String> createSet(String[] entries) {
		Set<String> set = new HashSet<String>();
		for (String entry : entries) set.add(entry);
		return set;
	}
	
	public static Map<String, LegRouter> createLegRouters(Config config, Network network, Map<String, TravelTime> travelTimes) {
		
		Set<String> modesToReroute = new HashSet<String>();
		modesToReroute.add(TransportMode.car);
		modesToReroute.add(TransportMode.ride);
		modesToReroute.add(TransportMode.bike);
		modesToReroute.add(TransportMode.walk);
		modesToReroute.add(TransportMode.pt);

		ModeRouteFactory modeRouteFactory = new ModeRouteFactory();
		modeRouteFactory.setRouteFactory(TransportMode.car, new LinkNetworkRouteFactory());
		modeRouteFactory.setRouteFactory(TransportMode.ride, new LinkNetworkRouteFactory());
		modeRouteFactory.setRouteFactory(TransportMode.bike, new LinkNetworkRouteFactory());
		modeRouteFactory.setRouteFactory(TransportMode.walk, new LinkNetworkRouteFactory());
		modeRouteFactory.setRouteFactory(TransportMode.pt, new LinkNetworkRouteFactory());
				
		// create Router Factory
		LeastCostPathCalculatorFactory routerFactory = new FastAStarLandmarksFactory(network, new FreespeedTravelTimeAndDisutility(config.planCalcScore()));

		Map<String, LegRouter> legRouters = new HashMap<String, LegRouter>();
				
		// Define restrictions for the different modes.
		// Car
		Set<String> carModeRestrictions = new HashSet<String>();
		carModeRestrictions.add(TransportMode.car);
		
		// Walk
		Set<String> walkModeRestrictions = new HashSet<String>();
		walkModeRestrictions.add(TransportMode.bike);
		walkModeRestrictions.add(TransportMode.walk);
				
		/*
		 * Bike
		 * Besides bike mode we also allow walk mode - but then the
		 * agent only travels with walk speed (handled in MultiModalTravelTimeCost).
		 */
		Set<String> bikeModeRestrictions = new HashSet<String>();
		bikeModeRestrictions.add(TransportMode.walk);
		bikeModeRestrictions.add(TransportMode.bike);
		
		/*
		 * PT
		 * We assume PT trips are possible on every road that can be used by cars.
		 * 
		 * Additionally we also allow pt trips to use walk and / or bike only links.
		 * On those links the traveltimes are quite high and we can assume that they
		 * are only use e.g. to walk from the origin to the bus station or from the
		 * bus station to the destination.
		 */
		Set<String> ptModeRestrictions = new HashSet<String>();
		ptModeRestrictions.add(TransportMode.pt);
		ptModeRestrictions.add(TransportMode.car);
		ptModeRestrictions.add(TransportMode.bike);
		ptModeRestrictions.add(TransportMode.walk);
		
		/*
		 * Ride
		 * We assume ride trips are possible on every road that can be used by cars.
		 * Additionally we also allow ride trips to use walk and / or bike only links.
		 * For those links walk travel times are used.
		 */
		Set<String> rideModeRestrictions = new HashSet<String>();
		rideModeRestrictions.add(TransportMode.car);
		rideModeRestrictions.add(TransportMode.bike);
		rideModeRestrictions.add(TransportMode.walk);
		
		TravelTime travelTime;
		TravelDisutility travelDisutility;
		LeastCostPathCalculator routeAlgo;
		TravelDisutilityFactory travlDisutilityFactory = new TravelCostCalculatorFactoryImpl();
		TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(network);
		for (String mode : modesToReroute) {
			
			Set<String> modeRestrictions;
			if (mode.equals(TransportMode.car)) {
				modeRestrictions = carModeRestrictions;
			}
			else if (mode.equals(TransportMode.walk)) {
				modeRestrictions = walkModeRestrictions;
			} else if (mode.equals(TransportMode.bike)) {
				modeRestrictions = bikeModeRestrictions;
			} else if (mode.equals(TransportMode.ride)) {
				modeRestrictions = rideModeRestrictions;
			} else if (mode.equals(TransportMode.pt)) {
				modeRestrictions = ptModeRestrictions;
			} else continue;
			
			Network subNetwork = NetworkImpl.createNetwork();
			networkFilter.filter(subNetwork, modeRestrictions);
			
			travelTime = travelTimes.get(mode); 
			
			travelDisutility = travlDisutilityFactory.createTravelDisutility(travelTime, config.planCalcScore());
			
			routeAlgo = routerFactory.createPathCalculator(subNetwork, travelDisutility, travelTime);
			legRouters.put(mode, new NetworkLegRouter(network, routeAlgo, modeRouteFactory));			
		}
		
		return legRouters;
	}
}
