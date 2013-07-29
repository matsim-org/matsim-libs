/* *********************************************************************** *
 * project: org.matsim.*
 * PED2012ScenarioCreation.java
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

package playground.gregor.scenariogen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

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
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.gregor.sim2d_v3.random.XORShiftRandom;

public class PED12ScenarioGenZurich {

	static final Logger log = Logger.getLogger(PED12ScenarioGenZurich.class);

//	public static String baseDir = "/home/cdobler/workspace/matsim/mysimulations/ped2012/";
	public static String baseDir = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/";
	
	public static String shoppingNetwork = baseDir + "input_2d/network.xml";
	public static String zurichNetwork = baseDir + "input_zh/network_ivtch.xml.gz";
	public static String mergedNetwork = baseDir + "input/network.xml.gz";
	public static String zurichPopulation = baseDir + "input_zh/plans_25pct.xml.gz";
	public static String mergedPopulation = baseDir + "input/plans.xml.gz";
	
	public static List<Link> shoppingShelfs = new ArrayList<Link>();
	public static List<Link> cashDesks = new ArrayList<Link>();
	
	/*package*/ static String modes = "car,walk,ride,bike,pt";
	/*package*/ static int numWalkAgents = 750;
	/*package*/ static int numCarAgents = 250;
	/*package*/ static Queue<Tuple<Id, Double>> parkingLotQueue;
	
	public static Random random = MatsimRandom.getLocalInstance();
	public static double dx = 683300;
	public static double dy = 248182.7969 - 8;	// y coordinate from Node 2796 in IVT's CH-EU planning network
	public static double da = -90;
	
	public static void main(String[] args) {

		// run shopping center generator to create shopping shelf and cash desk information
		PED12ScenarioGen.dx = dx;
		PED12ScenarioGen.dy = dy;
		PED12ScenarioGen.da = da;
		PED12ScenarioGen.EPSG = "EPSG: 4326";
		PED12ScenarioGen.main(args);
		
		// load shopping scenario
		Config shoppingConfig = ConfigUtils.createConfig();
		shoppingConfig.network().setInputFile(shoppingNetwork);
		Scenario shoppingScenario = ScenarioUtils.loadScenario(shoppingConfig);
		
		// load zurich scenario
		Config zurichConfig = ConfigUtils.createConfig();
		zurichConfig.network().setInputFile(zurichNetwork);
//		zurichConfig.plans().setInputFile(zurichPopulation);
		Scenario zurichScenario = ScenarioUtils.loadScenario(zurichConfig);
				
		// convert zurich network to multi-modal network
        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup3 = (MultiModalConfigGroup) zurichConfig.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup3 == null) {
            multiModalConfigGroup3 = new MultiModalConfigGroup();
            zurichConfig.addModule(multiModalConfigGroup3);
        }
        multiModalConfigGroup3.setCreateMultiModalNetwork(true);
        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup2 = (MultiModalConfigGroup) zurichConfig.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup2 == null) {
            multiModalConfigGroup2 = new MultiModalConfigGroup();
            zurichConfig.addModule(multiModalConfigGroup2);
        }
        multiModalConfigGroup2.setCutoffValueForNonCarModes(80 / 3.6);
        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup1 = (MultiModalConfigGroup) zurichConfig.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup1 == null) {
            multiModalConfigGroup1 = new MultiModalConfigGroup();
            zurichConfig.addModule(multiModalConfigGroup1);
        }
        multiModalConfigGroup1.setSimulatedModes(modes);
        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) zurichConfig.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup == null) {
            multiModalConfigGroup = new MultiModalConfigGroup();
            zurichConfig.addModule(multiModalConfigGroup);
        }
        new MultiModalNetworkCreator(multiModalConfigGroup).run(zurichScenario.getNetwork());
		
		// prepare Zurich population
		prepareZurichPopulation(zurichScenario);
		
		// merge networks
		for (Node node : shoppingScenario.getNetwork().getNodes().values()) {
			zurichScenario.getNetwork().addNode(node);
		}
		for (Link link : shoppingScenario.getNetwork().getLinks().values()) {
			/*
			 * Remove the link from in- and out-links map of its from- and to-node because they are
			 * re-added by the addLink method.
			 */
			link.getFromNode().getOutLinks().remove(link.getId());
			link.getToNode().getInLinks().remove(link.getId());
			zurichScenario.getNetwork().addLink(link);
		}
		
		// get shopping shelf and cash desk information and replace them with link objects from merged network
		for (Link link : PED12ScenarioGen.shoppingShelfs) {
			shoppingShelfs.add(zurichScenario.getNetwork().getLinks().get(link.getId()));
		}
		for (Link link : PED12ScenarioGen.cashDesks) {
			cashDesks.add(zurichScenario.getNetwork().getLinks().get(link.getId()));
		}
		
		// connect shopping and zurich network
		int nodeCount = 0;
		int linkCount = 0;
		Node node;
		Link link;
		Coord coord;

		Network zurichNetwork = zurichScenario.getNetwork();
		NetworkFactory networkFactory = zurichNetwork.getFactory();
		Node fromNode;
		Node toNode;
		Set<String> transportModes;

		// create road
		transportModes = CollectionUtils.stringToSet(modes);
		// people should *not* walk on the road to the shopping center
		transportModes.remove(TransportMode.walk);
		
		// connect incoming link
		fromNode = zurichNetwork.getNodes().get(zurichScenario.createId("2796"));
		toNode = zurichNetwork.getNodes().get(PED12ScenarioGen.carInNode);
		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), fromNode, toNode);
		link.setAllowedModes(transportModes);
		link.setFreespeed(50/3.6);
		link.setCapacity(1000.0);
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		zurichNetwork.addLink(link);
		
		// connect outgoing link
		fromNode = zurichNetwork.getNodes().get(PED12ScenarioGen.carOutNode);
		toNode = zurichNetwork.getNodes().get(zurichScenario.createId("3502"));
		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), fromNode, toNode);
		link.setAllowedModes(transportModes);
		link.setFreespeed(50/3.6);
		link.setCapacity(1000.0);
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		zurichNetwork.addLink(link);
		// create road
		
		// create walk-walk2d switch link
		transportModes = CollectionUtils.stringToSet(TransportMode.walk+",walk2d");
		
		// create a node shifted 1m south
		coord = zurichNetwork.getNodes().get(PED12ScenarioGen.walkInNode).getCoord();
		node = networkFactory.createNode(zurichScenario.createId("pedConnector" + nodeCount++), zurichScenario.createCoord(coord.getX(), coord.getY() - 1));
		zurichNetwork.addNode(node);
		
		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), node, zurichNetwork.getNodes().get(PED12ScenarioGen.walkInNode));
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		link.setAllowedModes(transportModes);
		link.setCapacity(10.0);
		link.setFreespeed(1.34);
		zurichNetwork.addLink(link);
		
		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), zurichNetwork.getNodes().get(PED12ScenarioGen.walkInNode), node);
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		link.setAllowedModes(transportModes);
		link.setCapacity(10.0);
		link.setFreespeed(1.34);
		zurichNetwork.addLink(link);
		// create walk-walk2d switch link
		
		// create pavement
		transportModes = CollectionUtils.stringToSet("walk");
		
		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), zurichNetwork.getNodes().get(zurichScenario.createId("2793")), node);
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		link.setAllowedModes(transportModes);
		link.setCapacity(10.0);
		link.setFreespeed(1.34);
		zurichNetwork.addLink(link);

		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), node, zurichNetwork.getNodes().get(zurichScenario.createId("2793")));
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		link.setAllowedModes(transportModes);
		link.setCapacity(10.0);
		link.setFreespeed(1.34);
		zurichNetwork.addLink(link);
		// create pavement
		
		// create walk-walk2d switch link
		transportModes = CollectionUtils.stringToSet("walk,walk2d");
		
		// create a node shifted 0.1m west
		coord = zurichNetwork.getNodes().get(PED12ScenarioGen.walkSubwayInNode).getCoord();
		node = networkFactory.createNode(zurichScenario.createId("pedConnector" + nodeCount++), zurichScenario.createCoord(coord.getX() - 0.1, coord.getY()));
		zurichNetwork.addNode(node);
		
		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), node, zurichNetwork.getNodes().get(PED12ScenarioGen.walkSubwayInNode));
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		link.setAllowedModes(transportModes);
		link.setCapacity(10.0);
		link.setFreespeed(1.34);
		zurichNetwork.addLink(link);
		
		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), zurichNetwork.getNodes().get(PED12ScenarioGen.walkSubwayInNode), node);
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		link.setAllowedModes(transportModes);
		link.setCapacity(10.0);
		link.setFreespeed(1.34);
		zurichNetwork.addLink(link);
		// create walk-walk2d switch link
		
		// connect shopping center with main station
		transportModes = CollectionUtils.stringToSet(TransportMode.walk);
		
		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), zurichNetwork.getNodes().get(zurichScenario.createId("3502")), node);
		// increase length by 25% to keep other walk connection attractive
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()) * 1.25);
		link.setAllowedModes(transportModes);
		link.setCapacity(10.0);
		link.setFreespeed(1.34);
		zurichNetwork.addLink(link);

		link = networkFactory.createLink(zurichScenario.createId("pedConnector" + linkCount++), node, zurichNetwork.getNodes().get(zurichScenario.createId("3502")));
		// increase length by 25% to keep other walk connection attractive
		link.setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()) * 1.25);
		link.setAllowedModes(transportModes);
		link.setCapacity(10.0);
		link.setFreespeed(1.34);
		zurichNetwork.addLink(link);
		// connect shopping center with main station
		
//		// convert network to multi-modal network
//		zurichConfig.multiModal().setCreateMultiModalNetwork(true);
//		zurichConfig.multiModal().setSimulatedModes("walk");
//		zurichConfig.multiModal().setCutoffValueForNonCarModes(80/3.6);
//		new MultiModalNetworkCreator(zurichConfig.multiModal()).run(zurichNetwork);

		// clean network
		new NetworkCleaner().run(zurichNetwork);
		
		// write merged network
		new NetworkWriter(zurichScenario.getNetwork()).write(mergedNetwork);
		PED12ScenarioGen.dumpNetworkAsShapeFile(zurichScenario, baseDir + "input");
		
		// create shopping population
		createPEDPopulation(zurichScenario);
		
		// write shopping population
		new PopulationWriter(zurichScenario.getPopulation(), zurichNetwork).writeV5(mergedPopulation);
	}
	
	/*
	 * Ensure that all activities can be reached.
	 * Update activity types from e.g. h12 to home.
	 * Drop non-NetworkRoutes.
	 */
	private static void prepareZurichPopulation(Scenario scenario) {

		Network network = scenario.getNetwork();		
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		multiModalConfigGroup.setSimulatedModes(TransportMode.bike + "," + TransportMode.pt + "," + TransportMode.ride + "," + TransportMode.walk);
		
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig());
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();

		Map<String, LegRouter> legRouters = createLegRouters(scenario.getConfig(), network, 
				multiModalTravelTimes);

		
		Counter removedPersons = new Counter ("removed persons: ");
		Iterator<? extends Person> iter = scenario.getPopulation().getPersons().values().iterator();
		while(iter.hasNext()) {
			Person person = iter.next();
			Plan plan = person.getSelectedPlan();
			for (int index = 0; index < plan.getPlanElements().size(); index++) {
				PlanElement planElement = plan.getPlanElements().get(index);
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					
					String type = activity.getType();
					if (type.startsWith("w")) {		
						activity.setType("work");
					} else if (type.startsWith("h")) {
						activity.setType("home");
					} else if (type.startsWith("s")) {
						activity.setType("shop");
					} else if (type.startsWith("l")) {
						activity.setType("leisure");
					} else if (type.startsWith("e")) {
						activity.setType("education");
					} else if (type.startsWith("t")) {
						activity.setType("tta");
					}
					else log.warn("Unknown activity type: " + type);
				} else if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					Route route = leg.getRoute();
					if (route != null && !(route instanceof NetworkRoute)) {
						leg.setRoute(null);
						
						Activity fromAct = (Activity) plan.getPlanElements().get(index-1);
						Activity toAct = (Activity) plan.getPlanElements().get(index+1);
						try {
							LegRouter legRouter = legRouters.get(leg.getMode());
							legRouter.routeLeg(person, leg, fromAct, toAct, leg.getDepartureTime());							
						}
						/*
						 * If no route is found, a RuntimeException is thrown.
						 * We catch it and remove the agent from the population.
						 */
						catch (java.lang.RuntimeException e) {
							iter.remove();
							removedPersons.incCounter();
							break;
						}
					}
				}
			}
		}
		removedPersons.printCounter();
	}
	
	private static void createPEDPopulation(Scenario scenario) {
		
		Network network = scenario.getNetwork();
		FreeSpeedTravelTime fs = new FreeSpeedTravelTime();
		TravelDisutility cost = new TravelCostCalculatorFactoryImpl().createTravelDisutility(fs, scenario.getConfig().planCalcScore());
		LeastCostPathCalculatorFactory routerFactory = new FastDijkstraFactory();
		Dijkstra dijkstra = (Dijkstra) routerFactory.createPathCalculator(network, cost, fs);
		Set<String> modes = new HashSet<String>();
		modes.add(TransportMode.walk);
		modes.add("walk2d");
		dijkstra.setModeRestriction(modes);
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		CreateWalk2DLegs createWalk2DLegs = new CreateWalk2DLegs(scenario);
		
		int pedCount = 0;
		
		for (int i = 0; i < numWalkAgents; i++) {
			
			Id id = scenario.createId("pedAgent" + pedCount++);
			PersonImpl pers = (PersonImpl) populationFactory.createPerson(id);
			Plan plan = populationFactory.createPlan();
			
			// set random age and gender
			pers.setAge((int)(10 + Math.round(random.nextDouble() * 60)));	// 10 .. 70 years old
			if (random.nextDouble() > 0.5) pers.setSex("m");
			else pers.setSex("f");
			
			// distance 250 ... 2750m to shopping center -> ensure that agents do not start with walk2d
			double homeX = dx + Math.signum(random.nextDouble() - 0.5) * (random.nextDouble() + 0.1) * 2500;
			double homeY = dy + Math.signum(random.nextDouble() - 0.5) * (random.nextDouble() + 0.1) * 2500;
			Coord homeCoord = scenario.createCoord(homeX, homeY);
			
			Link homeLink = ((NetworkImpl) scenario.getNetwork()).getNearestLink(homeCoord);
			homeCoord = homeLink.getCoord();
		
			ActivityImpl act = (ActivityImpl) populationFactory.createActivityFromLinkId("home", homeLink.getId());
			act.setCoord(homeCoord);
			// leave home between 08:00 and 16:00
			act.setEndTime(8 * 3600 + Math.round(8 * 3600 * random.nextDouble()));
			plan.addActivity(act);
			Leg leg = populationFactory.createLeg("walk");
			
			Route route = createRandomShoppingRoute(homeLink, dijkstra);
			leg.setRoute(route);
			
			plan.addLeg(leg);
			Activity act2 = populationFactory.createActivityFromLinkId("home", homeLink.getId());
			plan.addActivity(act2);
			pers.addPlan(plan);
			
			createWalk2DLegs.run(plan);
			
			scenario.getPopulation().addPerson(pers);
		}
		
		// manage parking lots
		if (numCarAgents > 0) {
			parkingLotQueue = new PriorityQueue<Tuple<Id,Double>>(numCarAgents, new ParkingComparator());
			for (Link link : network.getLinks().values()) {
				Id linkId = link.getId();
				if (linkId.toString().toLowerCase().contains("parking")) {
					/*
					 * Time when the parking lot becomes available:
					 * 08:00:00 + a random value to avoid that the parking
					 * lots are filled from first to last
					 */
					double t = 8*3600 + random.nextDouble() * 3600;
					parkingLotQueue.add(new Tuple<Id, Double>(linkId, t));
				}
			}
			
			double departureTime = 8 * 3600;
			double dt = 8 * 3600 / numCarAgents;
			for (int i = 0; i < numCarAgents; i++) {
				
				Id id = scenario.createId("pedAgent" + pedCount++);
				PersonImpl pers = (PersonImpl) populationFactory.createPerson(id);
				Plan plan = populationFactory.createPlan();
				
				// set random age and gender
				pers.setAge((int)(10 + Math.round(random.nextDouble() * 60)));	// 10 .. 70 years old
				if (random.nextDouble() > 0.5) pers.setSex("m");
				else pers.setSex("f");
				
				// distance 500 ... 5500m to shopping center -> ensure that agents do not start with walk2d
				double homeX = dx + Math.signum(random.nextDouble() - 0.5) * (random.nextDouble() + 0.1) * 5500;
				double homeY = dy + Math.signum(random.nextDouble() - 0.5) * (random.nextDouble() + 0.1) * 5500;
				Coord homeCoord = scenario.createCoord(homeX, homeY);
				
				Link homeLink = ((NetworkImpl) scenario.getNetwork()).getNearestLink(homeCoord);
				homeCoord = homeLink.getCoord();
				
				ActivityImpl act = (ActivityImpl) populationFactory.createActivityFromLinkId("home", homeLink.getId());
				act.setCoord(homeCoord);
				// leave home between 08:00 and 16:00
				departureTime += dt * random.nextDouble();
				act.setEndTime(departureTime);
				plan.addActivity(act);
				Leg leg = populationFactory.createLeg("car");
				
				Id parkingLinkId = parkingLotQueue.poll().getFirst();
				Link parkingLink = network.getLinks().get(parkingLinkId);
				Route route = createSimpleCarRoute(homeLink, parkingLink, dijkstra);
				leg.setRoute(route);
				plan.addLeg(leg);
				
				act = (ActivityImpl) populationFactory.createActivityFromLinkId("parking", parkingLinkId);
				act.setCoord(parkingLink.getCoord());
				act.setEndTime(0);
				plan.addActivity(act);
				
				Leg shopping = populationFactory.createLeg("walk2d");
				
				route = createRandomShoppingRoute(parkingLink, dijkstra);
				shopping.setRoute(route);
				plan.addLeg(shopping);
				
				act = (ActivityImpl) populationFactory.createActivityFromLinkId("parking", parkingLinkId);
				act.setEndTime(0);
				plan.addActivity(act);
				
				route = createSimpleCarRoute(parkingLink, homeLink, dijkstra);
				leg = populationFactory.createLeg("car");
				leg.setRoute(route);
				plan.addLeg(leg);
				
				act = (ActivityImpl) populationFactory.createActivityFromLinkId("home", homeLink.getId());
				plan.addActivity(act);
				pers.addPlan(plan);
				
				scenario.getPopulation().addPerson(pers);
				
				/* 
				 * Re-insert parking lot into the queue - assuming that it will
				 * be available at a later point in time. 
				 */
				parkingLotQueue.add(new Tuple<Id, Double>(parkingLinkId, departureTime + 600));
			}
		}
	}
	
	private static class ParkingComparator implements Comparator<Tuple<Id,Double>> {

		@Override
		public int compare(Tuple<Id, Double> t1, Tuple<Id, Double> t2) {
			if (t1.getSecond() < t2.getSecond()) return -1;
			else if (t1.getSecond() > t2.getSecond()) return 1;
			else return t1.getFirst().compareTo(t2.getFirst());
		}
		
	}
	
	private static Route createSimpleCarRoute(Link from, Link to, Dijkstra dijkstra) {
		
		dijkstra.setModeRestriction(CollectionUtils.stringToSet(TransportMode.car));
		Path r = dijkstra.calcLeastCostPath(from.getToNode(), to.getFromNode(), 0, null, null);
		
		List<Id> linkIds = new ArrayList<Id>();
		for (Link l : r.links) {
			linkIds.add(l.getId());
		}

		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(from.getId(), to.getId());
		route.setLinkIds(from.getId(), linkIds, to.getId());

		dijkstra.setModeRestriction(null);
		return route;
	}
	
	private static Route createRandomShoppingRoute(Link link, Dijkstra dijkstra) {
		
		dijkstra.setModeRestriction(CollectionUtils.stringToSet("walk2d,walk"));
		int stopsAtShoppingShelfs = random.nextInt(3) + 1; // three stops max
		
		Link current = link;
		
		List<Link> links = new ArrayList<Link>();

		// add home link to ensure that the agents starts its route there
		links.add(link);
		
		for (int stop = 0; stop < stopsAtShoppingShelfs; stop++) {
			
			Link next = shoppingShelfs.get(MatsimRandom.getRandom().nextInt(shoppingShelfs.size()));
			Node from = current.getToNode();
			Node to = next.getFromNode();
			
			Path r = dijkstra.calcLeastCostPath(from, to, 0, null, null);
			links.addAll(r.links);
			links.add(next);
			current = next;
		}
				
		Link cashDesk = cashDesks.get(MatsimRandom.getRandom().nextInt(cashDesks.size()));
		Node from = current.getToNode();
		Node to = cashDesk.getFromNode();
		
		Path r = dijkstra.calcLeastCostPath(from, to, 0, null, null);
		links.addAll(r.links);
		links.add(cashDesk);
		current = cashDesk;		
		
		from = current.getToNode();
		to = link.getFromNode();
		
		r = dijkstra.calcLeastCostPath(from, to, 0, null, null);
		links.addAll(r.links);
		current = cashDesk;
		
		// add home link link to ensure that the agents ends its route there
		links.add(link);
		
		List<Id> linkIds = new ArrayList<Id>();
		for (Link l : links) {
			linkIds.add(l.getId());
		}
		
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(links.get(0).getId(), links.get(links.size()-1).getId());
		route.setLinkIds(links.get(0).getId(), linkIds.subList(1, links.size()-1), links.get(links.size()-1).getId());
		
		return route;
	}
	
	/*
	 * Splits walk legs into walk and walk2d legs. Separate them with a
	 * switchWalkMode activity with duration 0.
	 */
	public static class CreateWalk2DLegs implements PlanAlgorithm {

		private final Scenario scenario;
		private final PopulationFactory populationFactory;
		
		public CreateWalk2DLegs(Scenario scenario) {
			this.scenario = scenario;
			this.populationFactory = scenario.getPopulation().getFactory();
		}
		
		@Override
		public void run(Plan plan) {
			List<Leg> walkLegs = new ArrayList<Leg>();
			
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (leg.getMode().equals(TransportMode.walk)) {
						walkLegs.add(leg);
					}
				}
			}
			
			for (Leg walkLeg : walkLegs) {		
				NetworkRoute route = (NetworkRoute) walkLeg.getRoute();
				List<PlanElement> replacement = new ArrayList<PlanElement>();
				
//				List<Id> linkIds = route.getLinkIds();
				List<Id> linkIds = new ArrayList<Id>();
				linkIds.add(route.getStartLinkId());
				linkIds.addAll(route.getLinkIds());
				linkIds.add(route.getEndLinkId());
				
				String currentMode = null;
				int startRouteIndex = 0;
				Link startLink =  scenario.getNetwork().getLinks().get(linkIds.get(0));
				if (startLink.getAllowedModes().contains("walk2d")) currentMode = "walk2d";
				else currentMode = TransportMode.walk;
				
				for (int i = 0; i < route.getLinkIds().size(); i++) {
					Link link = scenario.getNetwork().getLinks().get(linkIds.get(i));
					
					boolean switchMode = false;
					String newMode = null;
					
					// current mode is walk but link supports walk2d
					if (currentMode.equals(TransportMode.walk) && link.getAllowedModes().contains("walk2d")) {
						switchMode = true;
						newMode = "walk2d";
					}
					// current mode is walk2d but link does not support
					else if (currentMode.equals("walk2d") && !link.getAllowedModes().contains("walk2d")) {
						switchMode = true;
						newMode = TransportMode.walk;
					}
					
					// if the mode switches...
					if (switchMode) {
						
						int endRouteIndex;
						
						// if switching to walk2d
						if (currentMode.equals(TransportMode.walk)) endRouteIndex = i; 
						// if switching to walk
						else endRouteIndex = i - 1;
						
						/*
						 * Create normal vector to create random start position for the agent.
						 * Shift the agent +/- 1m.
						 */
//						Link activityLink = scenario.getNetwork().getLinks().get(linkIds.get(endRouteIndex + 1));
//						Coord fromNodeCoord = activityLink.getFromNode().getCoord();
//						Coord toNodeCoord = activityLink.getToNode().getCoord();
//						double dx = toNodeCoord.getX() - fromNodeCoord.getX();
//						double dy = toNodeCoord.getY() - fromNodeCoord.getY();
//						double l = Math.sqrt(dx*dx + dy*dy);
//						dx = dx/l;
//						dy = dy/l;
//						double rand = random.nextDouble()* 2 - 1;	// random double between -1 and 1
//						Coord activityCoord = scenario.createCoord(toNodeCoord.getX() + , y)
											
						Leg leg = populationFactory.createLeg(currentMode);					
						NetworkRoute subRoute = route.getSubRoute(linkIds.get(startRouteIndex), linkIds.get(endRouteIndex)); 
						leg.setRoute(subRoute);
						replacement.add(leg);
						
						/*
						 * Create a coordinate for the switch activity. We want the activity to be located
						 * near to the toNode of the switch link but we cannot take its exact coordinate
						 * since two agents switching in the same time step would be placed at the same
						 * position what would result in NaNs in some force calculation modules.
						 */
						Coord coord = scenario.getNetwork().getLinks().get(linkIds.get(endRouteIndex + 1)).getCoord();	// links toNode coordinate
						XORShiftRandom xor = new XORShiftRandom((long) plan.getPerson().getId().hashCode());
						double x = coord.getX() + xor.nextDouble() - 1.0;	// +/- 0.5m
						double y = coord.getY() + xor.nextDouble() - 1.0;	// +/- 0.5m
						
						ActivityImpl act = (ActivityImpl) populationFactory.createActivityFromLinkId("switchWalkMode", linkIds.get(endRouteIndex));
						act.setMaximumDuration(0.0);
						act.setCoord(scenario.createCoord(x, y));
						replacement.add(act);
						
						startRouteIndex = endRouteIndex;
						currentMode = newMode;
					}			
				}
				
				// create leg for remaining links in the route
				Leg leg = populationFactory.createLeg(currentMode);					
				NetworkRoute subRoute = route.getSubRoute(linkIds.get(startRouteIndex), linkIds.get(linkIds.size() - 1)); 
				leg.setRoute(subRoute);
				replacement.add(leg);

				int legIndex = plan.getPlanElements().indexOf(walkLeg);
				plan.getPlanElements().remove(legIndex);
				plan.getPlanElements().addAll(legIndex, replacement);
			}
		}
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