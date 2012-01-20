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

package org.matsim.ptproject.qsim.multimodalsimengine.router;

import java.util.HashSet;
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
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.BikeTravelTimeFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.MultiModalTravelTime;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.PTTravelTimeFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.RideTravelTimeFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.TravelTimeFactoryWrapper;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.WalkTravelTimeFactory;
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
		TravelTimeFactoryWrapper wrapper = new TravelTimeFactoryWrapper(travelTimeCalculator);

		PlansCalcRouteConfigGroup configGroup = config.plansCalcRoute();
		MultiModalTravelTimeWrapperFactory multiModalTravelTimeFactory = new MultiModalTravelTimeWrapperFactory();
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, wrapper);
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.walk, new WalkTravelTimeFactory(configGroup));
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.bike, new BikeTravelTimeFactory(configGroup, new WalkTravelTimeFactory(configGroup)));
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.ride, new RideTravelTimeFactory(wrapper, new WalkTravelTimeFactory(configGroup)));
		multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.pt, new PTTravelTimeFactory(configGroup, wrapper, new WalkTravelTimeFactory(configGroup)));
		MultiModalTravelTime travelTime = multiModalTravelTimeFactory.createTravelTime();	
		
		/*
		 * Create travel cost object
		 */
		TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
		PersonalizableTravelCost travelCost = travelCostCalculatorFactory.createTravelCostCalculator(travelTime, config.planCalcScore());
		
		
		IntermodalLeastCostPathCalculator routeAlgo = (IntermodalLeastCostPathCalculator) new DijkstraFactory().createPathCalculator(scenario.getNetwork(), travelCost, travelTime);
		
		MultiModalLegRouter multiModalLegRouter = new MultiModalLegRouter(scenario.getNetwork(), travelTime, travelCost, routeAlgo);
		
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
		multiModalLegRouter.routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.car}), leg, scenario.getNetwork());
		
		/*
		 * check pt mode
		 */
		transportMode = TransportMode.pt;
		leg.setMode(transportMode);
		multiModalLegRouter.routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.car, TransportMode.pt}), leg, scenario.getNetwork());
		
		/*
		 * check walk mode
		 */
		transportMode = TransportMode.walk;
		leg.setMode(transportMode);
		multiModalLegRouter.routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.walk, TransportMode.bike}), leg, scenario.getNetwork());
		
		/*
		 * check bike mode
		 */
		transportMode = TransportMode.bike;
		leg.setMode(transportMode);
		multiModalLegRouter.routeLeg(person, leg, startActivity, endActivity, 0.0);
		checkRoute(createSet(new String[]{TransportMode.walk, TransportMode.bike}), leg, scenario.getNetwork());
		
		/*
		 * check ride mode
		 */
		transportMode = TransportMode.ride;
		leg.setMode(transportMode);
		multiModalLegRouter.routeLeg(person, leg, startActivity, endActivity, 0.0);
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
}
