/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalTripRouterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal;

import com.google.inject.name.Names;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.util.LinkSlopesReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeInterpretationModule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author cdobler
 */
public class MultiModalTripRouterTest {

	@Test
	void testRouteLeg() {

		final Config config = ConfigUtils.createConfig();
		config.routing().addParam("teleportedModeSpeed_bike", "6.01");
		config.routing().addParam("teleportedModeFreespeedFactor_pt", "2.0");
		config.routing().addParam("teleportedModeSpeed_ride", "15.0");
		config.routing().addParam("teleportedModeSpeed_undefined", "13.88888888888889");
		config.routing().addParam("teleportedModeSpeed_walk", "1.34");
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		config.scoring().addModeParams( new ScoringConfigGroup.ModeParams( TransportMode.ride ) );
		final Scenario scenario = ScenarioUtils.createScenario(config);

		createNetwork(scenario);

		MultiModalConfigGroup multiModalConfigGroup = new MultiModalConfigGroup();
		config.addModule(multiModalConfigGroup);
		multiModalConfigGroup.setSimulatedModes(TransportMode.bike + ", " + TransportMode.walk + ", " + TransportMode.ride + ", " + TransportMode.pt);

		Map<Id<Link>, Double> linkSlopes = new LinkSlopesReader().getLinkSlopes(multiModalConfigGroup, scenario.getNetwork());

		/*
		 * Use a ...
		 * - defaultDelegateFactory for the QNetsim modes
		 * - multiModalTripRouterFactory for the multi-modal modes
		 * - transitTripRouterFactory for transit trips
		 *
		 * Note that a FastDijkstraFactory is used for the multiModalTripRouterFactory
		 * since ...
		 * - only "fast" router implementations handle sub-networks correct
		 * - AStarLandmarks uses link speed information instead of agent speeds
		 */

		MultiModalModule multiModalModule = new MultiModalModule();
		multiModalModule.setLinkSlopes(linkSlopes);
		com.google.inject.Injector injector = Injector.createInjector(
				config,
				AbstractModule.override(Collections.singleton(new AbstractModule() {
					@Override
					public void install() {
						bind(EventsManager.class).toInstance(EventsUtils.createEventsManager(config));
						install(new ScenarioByInstanceModule(scenario));
						install(new TripRouterModule());
						install(new TravelTimeCalculatorModule());
						install(new TravelDisutilityModule());
						install(new TimeInterpretationModule());
						bind(Integer.class).annotatedWith(Names.named("iteration")).toInstance(0);
					}
				}), multiModalModule));

		TripRouter tripRouter = injector.getInstance(TripRouter.class);
		PlanRouter planRouter = new PlanRouter(tripRouter, injector.getInstance(TimeInterpretation.class));

		/*
		 * Create travel time object
		 */
		// pre-initialize the travel time calculator to be able to use it in the wrapper
//		TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
//		TravelTimeCalculator travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(scenario.getNetwork(), config.travelTimeCalculator());

//		PlansCalcRouteConfigGroup configGroup = config.plansCalcRoute();
//		Map<String, TravelTime> multiModalTravelTimes = new HashMap<String, TravelTime>();
//		multiModalTravelTimes.put(TransportMode.car, travelTimeCalculator.getLinkTravelTimes());
//		multiModalTravelTimes.put(TransportMode.walk, new WalkTravelTimeOld(configGroup));
//		multiModalTravelTimes.put(TransportMode.bike, new BikeTravelTimeOld(configGroup, new WalkTravelTimeOld(configGroup)));
//		multiModalTravelTimes.put(TransportMode.ride, new RideTravelTime(travelTimeCalculator.getLinkTravelTimes(), new WalkTravelTimeOld(configGroup)));
//		multiModalTravelTimes.put(TransportMode.pt, new PTTravelTime(configGroup, travelTimeCalculator.getLinkTravelTimes(), new WalkTravelTimeOld(configGroup)));

//		Map<String, LegRouter> legRouters = createLegRouters(config, scenario.getNetwork(), multiModalTravelTimes);



		/*
		 * check car mode
		 */
		checkMode(scenario, TransportMode.car, planRouter);

		/*
		 * check pt mode
		 */
		checkMode(scenario, TransportMode.pt, planRouter);

		/*
		 * check walk mode
		 */
		checkMode(scenario, TransportMode.walk, planRouter);

		/*
		 * check bike mode
		 */
		checkMode(scenario, TransportMode.bike, planRouter);

		/*
		 * check ride mode
		 */
		checkMode(scenario, TransportMode.ride, planRouter);
	}

	private void checkMode(Scenario scenario, String transportMode, PlanRouter planRouter) {
		// XXX transportMode parameter is NOT USED, hence this test is NOT DOING WHAT IT SHOULD! td oct 15
		Person person = createPerson(scenario);
		planRouter.run(person);
		checkRoute((Leg) person.getSelectedPlan().getPlanElements().get(1), scenario.getNetwork());
	}

	private Person createPerson(Scenario scenario) {

		Person person = scenario.getPopulation().getFactory().createPerson(Id.create("person", Person.class));
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		person.addPlan(plan);

		Activity startActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("start", Id.create("startLink", Link.class));
		startActivity.setEndTime(8*3600);
		Leg leg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		Activity endActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("end", Id.create("endLink", Link.class));

		plan.addActivity(startActivity);
		plan.addLeg(leg);
		plan.addActivity(endActivity);

		return person;
	}


	private void checkRoute(Leg leg, Network network) {
		NetworkRoute route = (NetworkRoute) leg.getRoute();

		for (Id<Link> id : route.getLinkIds()) {
			Link link = network.getLinks().get(id);
			boolean validMode = false;
			for (String transportMode : link.getAllowedModes()) {
				if (link.getAllowedModes().contains(transportMode)) {
					validMode = true;
					break;
				}
			}
			Assertions.assertTrue(validMode);

		}
	}

	private void createNetwork(Scenario scenario) {

		/*
		 * create nodes
		 */
		Node startNode = scenario.getNetwork().getFactory().createNode(Id.create("startNode", Node.class), new Coord(0.0, 0.0));
		Node splitNode = scenario.getNetwork().getFactory().createNode(Id.create("splitNode", Node.class), new Coord(1.0, 0.0));

		Node carNode = scenario.getNetwork().getFactory().createNode(Id.create("carNode", Node.class), new Coord(2.0, 2.0));
		Node ptNode = scenario.getNetwork().getFactory().createNode(Id.create("ptNode", Node.class), new Coord(2.0, 1.0));
		Node walkNode = scenario.getNetwork().getFactory().createNode(Id.create("walkNode", Node.class), new Coord(2.0, 0.0));
		double y1 = -1.0;
		Node bikeNode = scenario.getNetwork().getFactory().createNode(Id.create("bikeNode", Node.class), new Coord(2.0, y1));
		double y = -2.0;
		Node rideNode = scenario.getNetwork().getFactory().createNode(Id.create("rideNode", Node.class), new Coord(2.0, y));

		Node joinNode = scenario.getNetwork().getFactory().createNode(Id.create("joinNode", Node.class), new Coord(1.0, 0.0));
		Node endNode = scenario.getNetwork().getFactory().createNode(Id.create("endNode", Node.class), new Coord(0.0, 0.0));

		/*
		 * create links
		 */
		Link startLink = scenario.getNetwork().getFactory().createLink(Id.create("startLink", Link.class), startNode, splitNode);

		Link toCarLink = scenario.getNetwork().getFactory().createLink(Id.create("toCarLink", Link.class), splitNode, carNode);
		Link toPtLink = scenario.getNetwork().getFactory().createLink(Id.create("toPtLink", Link.class), splitNode, ptNode);
		Link toWalkLink = scenario.getNetwork().getFactory().createLink(Id.create("toWalkLink", Link.class), splitNode, walkNode);
		Link toBikeLink = scenario.getNetwork().getFactory().createLink(Id.create("toBikeLink", Link.class), splitNode, bikeNode);
		Link toRideLink = scenario.getNetwork().getFactory().createLink(Id.create("toRideLink", Link.class), splitNode, rideNode);

		Link fromCarLink = scenario.getNetwork().getFactory().createLink(Id.create("fromCarLink", Link.class), carNode, joinNode);
		Link fromPtLink = scenario.getNetwork().getFactory().createLink(Id.create("fromPtLink", Link.class), ptNode, joinNode);
		Link fromWalkLink = scenario.getNetwork().getFactory().createLink(Id.create("fromWalkLink", Link.class),  walkNode, joinNode);
		Link fromBikeLink = scenario.getNetwork().getFactory().createLink(Id.create("fromBikeLink", Link.class), bikeNode, joinNode);
		Link fromRideLink = scenario.getNetwork().getFactory().createLink(Id.create("fromRideLink", Link.class), rideNode, joinNode);

		Link endLink = scenario.getNetwork().getFactory().createLink(Id.create("endLink", Link.class), joinNode, endNode);

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
		Set<String> set = new HashSet<>();
        Collections.addAll(set, entries);
		return set;
	}
}
