/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalDemo.java
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

package playground.christoph.dissertation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.OnlyTravelDependentScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

import playground.christoph.evacuation.trafficmonitoring.BikeTravelTimeFactory;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTimeFactory;

/**
 * Demonstrate the influence of age and gender on persons' walk speed.
 * 
 * Scenario:
 * <ul>
 * 	<li>Have two routes which connect a start and an end link.</li>
 * 	<li>One is car (long), one is walk (short).</li>
 * 	<li>Choose their length so that a 30 year old male's is walk travel time
 * 		exactly matches the car travel time.</li> 
 * 	<li>Create 1000 agents with random age between 1 and 100 and random gender.</li>
 * 	<li>Create two initial populations: one with initial car routes, one with
 * 		initial walk route.</li>
 * 	<li>Repeat the experiment but replace walk by bike.</li>
 * </ul>
 *  @author cdobler
 */
public class MultiModalDemo {
	
	private static final Logger log = Logger.getLogger(MultiModalDemo.class);
	
	private static int numIterations = 250;
	private static int numPersons = 2000;
	private static String initialLegMode = TransportMode.car;
	private static String nonCarMode = TransportMode.walk;
//	private static String legModes = TransportMode.car + "," + TransportMode.bike + "," + TransportMode.walk;
	private static String legModes = TransportMode.car + "," + TransportMode.walk;
	private static double capacity = 10000.0;
	private static int departureDelay = 10;	// time between the departure of two agents

	private static double referenceCarSpeed = 50.0/3.6;
	private static String referenceSex = "m";
	private static int referenceAge = 30;
	
	/*
	 * If you have to adapt this, then something seems to be wrong! 
	 */
	private static final double expectedReferenceTravelTime = 676.0;	// walk, m, 30
	
	public static void main(String[] args) {
		
		// create and initialze config
		Config config = ConfigUtils.createConfig();
		
		QSimConfigGroup qSimConfigGroup = new QSimConfigGroup();
		qSimConfigGroup.setNumberOfThreads(1);
		qSimConfigGroup.setStartTime(0.0);
		qSimConfigGroup.setEndTime(86400.0);
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setRemoveStuckVehicles(false);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setVehicleBehavior(QSimConfigGroup.VEHICLE_BEHAVIOR_EXCEPTION);
		config.addQSimConfigGroup(qSimConfigGroup);
		
		config.multiModal().setCreateMultiModalNetwork(false);
		config.multiModal().setDropNonCarRoutes(false);
		config.multiModal().setNumberOfThreads(1);
		config.multiModal().setMultiModalSimulationEnabled(true);
		config.multiModal().setSimulatedModes(TransportMode.car + "," + TransportMode.bike + "," + TransportMode.walk);
			
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(numIterations);
		config.controler().setMobsim(ControlerConfigGroup.MobsimType.qsim.toString());
		config.controler().setOutputDirectory("../../matsim/mysimulations/dissertation/MultiModalDemo");
		
		Set<String> networkRouteModes = CollectionUtils.stringToSet(TransportMode.car + "," + TransportMode.bike + "," + TransportMode.walk);
		config.plansCalcRoute().setNetworkModes(networkRouteModes);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);
		config.plansCalcRoute().setWalkSpeed(1.34);
		config.plansCalcRoute().setBikeSpeed(6.01);
		config.plansCalcRoute().setPtSpeedFactor(2.0);
		
		config.strategy().setMaxAgentPlanMemorySize(4);
		config.strategy().addParam("Module_1", "ChangeLegMode");
		config.strategy().addParam("ModuleProbability_1", "0.10");
//		config.strategy().addParam("Module_2", "SelectExpBeta");
		config.strategy().addParam("Module_2", "BestScore");
		config.strategy().addParam("ModuleProbability_2", "0.90");
		
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(8*3600);
		config.planCalcScore().addActivityParams(home);
		
		Module module = config.createModule("changeLegMode");
		module.addParam("modes", legModes);	
			
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		createPopulation(scenario);
		
		adaptNetwork(scenario);
		
		Controler controler = new MultiModalDemoControler(scenario);
		controler.setOverwriteFiles(true);
		
		TravelTimeAnalyzer travelTimeAnalyzer = new TravelTimeAnalyzer();
		controler.getEvents().addHandler(travelTimeAnalyzer);
		controler.addControlerListener(travelTimeAnalyzer);
		
		controler.run();
		
		calculatePopulationStatistics(scenario);
		
		calculateExpectedModeShare(config);
	}
	
	private static void createNetwork(Scenario scenario) {
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		
		Node n1 = networkFactory.createNode(scenario.createId("n1"), scenario.createCoord(0.0, 0.0));
		Node n2 = networkFactory.createNode(scenario.createId("n2"), scenario.createCoord(100.0, 0.0));
		Node n3 = networkFactory.createNode(scenario.createId("n3"), scenario.createCoord(100.0, 1000.0));
		Node n4 = networkFactory.createNode(scenario.createId("n4"), scenario.createCoord(1100.0, 1000.0));
		Node n5 = networkFactory.createNode(scenario.createId("n5"), scenario.createCoord(1100.0, 0.0));
		Node n6 = networkFactory.createNode(scenario.createId("n6"), scenario.createCoord(1200.0, 0.0));
		
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		scenario.getNetwork().addNode(n4);
		scenario.getNetwork().addNode(n5);
		scenario.getNetwork().addNode(n6);
		
		Set<String> carMode = new HashSet<String>();
		Set<String> multiMode = new HashSet<String>();
		Set<String> nonCarMode = new HashSet<String>();
		
		carMode.add(TransportMode.car);
		nonCarMode.add(TransportMode.bike);
		nonCarMode.add(TransportMode.walk);
		multiMode.add(TransportMode.car);
		multiMode.add(TransportMode.bike);
		multiMode.add(TransportMode.walk);
	
		Link l1 = networkFactory.createLink(scenario.createId("l1"), n1, n2);
		l1.setLength(100.0);
		l1.setCapacity(capacity);
		l1.setFreespeed(referenceCarSpeed);
		l1.setAllowedModes(multiMode);
		
		Link l2 = networkFactory.createLink(scenario.createId("l2"), n2, n3);
		l2.setLength(1000.0);
		l2.setCapacity(capacity);
		l2.setFreespeed(referenceCarSpeed);
		l2.setAllowedModes(carMode);
		
		Link l3 = networkFactory.createLink(scenario.createId("l3"), n3, n4);
		l3.setLength(1000.0);
		l3.setCapacity(capacity);
		l3.setFreespeed(referenceCarSpeed);
		l3.setAllowedModes(carMode);

		Link l4 = networkFactory.createLink(scenario.createId("l4"), n4, n5);
		l4.setLength(1000.0);
		l4.setCapacity(capacity);
		l4.setFreespeed(referenceCarSpeed);
		l4.setAllowedModes(carMode);

		Link l5 = networkFactory.createLink(scenario.createId("l5"), n2, n5);
		l5.setLength(1000.0);
		l5.setCapacity(capacity);
		l5.setFreespeed(referenceCarSpeed);
		l5.setAllowedModes(nonCarMode);
		
		Link l6 = networkFactory.createLink(scenario.createId("l6"), n5, n6);
		l6.setLength(100.0);
		l6.setCapacity(capacity);
		l6.setFreespeed(referenceCarSpeed);
		l6.setAllowedModes(multiMode);
		
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);
		scenario.getNetwork().addLink(l4);
		scenario.getNetwork().addLink(l5);
		scenario.getNetwork().addLink(l6);
	}
	
	private static void adaptNetwork(Scenario scenario) {
		
		double refNonCarTravelTime = calculateNonCarTravelTime(scenario.getConfig());
		
		adaptLinkLength(scenario, refNonCarTravelTime);
		
		double refCarTravelTime = calculateCarTravelTime(scenario.getConfig(), refNonCarTravelTime);
		
		if (refCarTravelTime != refNonCarTravelTime) {
			throw new RuntimeException("Reference car travel time (" + refCarTravelTime + ") " +
					"does not match reference non-car travel time (" + refNonCarTravelTime  + ")!");
		}
		if (expectedReferenceTravelTime != refNonCarTravelTime) {
			throw new RuntimeException("Expected reference travel time (" + expectedReferenceTravelTime + ") " +
					"does not match reference non-car travel time (" + refNonCarTravelTime  + ")!");
		}
	}
	
	private static void adaptLinkLength(Scenario scenario, double refNonCarTravelTime) {
		double adaptiveLinkLength = calculateLinkLength(scenario, refNonCarTravelTime);
		Link l3 = scenario.getNetwork().getLinks().get(scenario.createId("l3"));
		l3.setLength(adaptiveLinkLength);
	}
	
	/*
	 * Calculate the length of the links which an agent has to travel by car to
	 * have the same travel time as an agent walking the other route.
	 */
	private static double calculateLinkLength(Scenario scenario, double refNonCarTravelTime) {
		TravelTime carTravelTime = new FreeSpeedTravelTime();
		
		/*
		 * Car travel time along the unchanged links.
		 * QLinkImpl uses Math.floor(...) for earliest exit times.
		 * 
		 * The agent needs one second to leave link l1. For every further link,
		 * additional one second is needed because the agent is moved from the
		 * buffer to the outgoing queue, which takes one second.
		 */
		Link l2 = scenario.getNetwork().getLinks().get(scenario.createId("l2"));
		Link l4 = scenario.getNetwork().getLinks().get(scenario.createId("l4"));
		Link l6 = scenario.getNetwork().getLinks().get(scenario.createId("l6"));
 		double minCarTT = 1.0 + Math.floor(carTravelTime.getLinkTravelTime(l2, 0.0, null, null)) +
			Math.floor(carTravelTime.getLinkTravelTime(l4, 0.0, null, null)) +
			Math.floor(carTravelTime.getLinkTravelTime(l6, 0.0, null, null)) + 3.0;
		
		double deltaCarTT = refNonCarTravelTime - minCarTT;
				
		if (deltaCarTT < 0) {
			throw new RuntimeException("Found negative travel time for link. " +
					"Something seems to be wrong here - this should not happen!");
		} else {
			double length = deltaCarTT * referenceCarSpeed;
			log.info("Found length of " + length + " m for adaptive car link.");
			return length;
		}
	}
	
	private static double calculateCarTravelTime(Config config, double refNonCarTravelTime) {
		
		// adapt config
		config.controler().setRunId("reference_car");
		config.controler().setLastIteration(0);
		
		Scenario sc = ScenarioUtils.createScenario(config);
		createNetwork(sc);
		adaptLinkLength(sc, refNonCarTravelTime);
		
		// create reference population
		Person person = createPerson(sc, sc.createId("1"), referenceSex, referenceAge, 0.0, TransportMode.car);
		sc.getPopulation().addPerson(person);
		
		// create routes
		Controler controler = new MultiModalDemoControler(sc);
		controler.setOverwriteFiles(true);
		
		TravelTimeAnalyzer travelTimeAnalyzer = new TravelTimeAnalyzer();
		controler.getEvents().addHandler(travelTimeAnalyzer);
		controler.addControlerListener(travelTimeAnalyzer);
		
		controler.run();
		
		// revert config
		config.controler().setRunId(null);
		config.controler().setLastIteration(numIterations);
		
		double travelTime = travelTimeAnalyzer.means.get(TransportMode.car).get(0);
		log.info("Found car travel time of " + travelTime);
		
		return travelTime;
	}
	
	private static double calculateNonCarTravelTime(Config config) {
		
		// adapt config
		config.controler().setLastIteration(0);
		config.controler().setRunId("reference_non_car");
		
		Scenario sc = ScenarioUtils.createScenario(config);
		createNetwork(sc);
		
		// create reference population
		for (int i = 0; i < 5000; i++) {
			Person person = createPerson(sc, sc.createId(String.valueOf(i)), referenceSex, referenceAge, 0.0, nonCarMode);
			sc.getPopulation().addPerson(person);
		}
		
		// create routes
		Controler controler = new MultiModalDemoControler(sc);
		controler.setOverwriteFiles(true);
		
		TravelTimeAnalyzer travelTimeAnalyzer = new TravelTimeAnalyzer();
		controler.getEvents().addHandler(travelTimeAnalyzer);
		controler.addControlerListener(travelTimeAnalyzer);
		
		controler.run();
		
		// revert config
		config.controler().setRunId(null);
		config.controler().setLastIteration(numIterations);
		
		/*
		 * We have to use the median travel time because |dt(v + 0.1)| != |dt(v - 0.1)|. 
		 */
		double travelTime = travelTimeAnalyzer.medians.get(nonCarMode).get(0);
		log.info("Found non-car travel time of " + travelTime);
		
		return travelTime;
	}
	
	private static void createPopulation(Scenario scenario) {
		Random random = MatsimRandom.getLocalInstance();
		
		for (int i = 0; i < numPersons; i++) {
			
			String sex;
			if (random.nextDouble() > 0.5) sex = "f";
			else sex = "m";
			
			int age = 1 + random.nextInt(100);
			
			Id personId = scenario.createId(String.valueOf(i));
			double departureTime = 8*3600 + i*departureDelay;

//			Person person = createPerson(scenario, personId, referenceSex, referenceAge, departureTime, initialLegMode);
			Person person = createPerson(scenario, personId, sex, age, departureTime, initialLegMode);
			
			scenario.getPopulation().addPerson(person);
		}
	}
	
	private static Person createPerson(Scenario scenario, Id id, String sex, int age, double departureTime, String legMode) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		Person person = populationFactory.createPerson(id);
		
		((PersonImpl) person).setSex(sex);
		((PersonImpl) person).setAge(age);
		((PersonImpl) person).setCarAvail("always");
		
		Plan plan = populationFactory.createPlan();
		
		Activity a1 = populationFactory.createActivityFromLinkId("home", scenario.createId("l1"));
		((ActivityImpl) a1).setCoord(scenario.getNetwork().getLinks().get(scenario.createId("l1")).getCoord());
		a1.setEndTime(departureTime);
		
		Leg l1 = populationFactory.createLeg(legMode);
		l1.setDepartureTime(departureTime);
		
		Activity a2 = populationFactory.createActivityFromLinkId("home", scenario.createId("l6"));
		((ActivityImpl) a2).setCoord(scenario.getNetwork().getLinks().get(scenario.createId("l6")).getCoord());
		
		plan.addActivity(a1);
		plan.addLeg(l1);
		plan.addActivity(a2);
		
		person.addPlan(plan);
		
		return person;
	}
	
	private static void calculatePopulationStatistics(Scenario scenario) {
		
		int car = 0;
		int nonCar = 0;
		
		double[] ages = new double[numPersons];
		int males = 0;
		int females = 0;
		
		int i = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			ages[i] = ((PersonImpl) person).getAge();
			if(((PersonImpl) person).getSex().equals("m")) males++;
			else females++;
			
			Leg leg = (Leg) ((PersonImpl) person).getBestPlan().getPlanElements().get(1);
			if (leg.getMode().equals(TransportMode.car)) car++;
			else nonCar++;
			
			i++;
		}
		
		log.info("Found " + males + " male agents (" + (1.0*males)/numPersons + "%).");
		log.info("Found " + females + " female agents (" + (1.0*females)/numPersons + "%).");
		log.info("Found mean age of " + StatUtils.percentile(ages, 50));
		log.info("Found " + car + " agents where car is the best transport mode.");
		log.info("Found " + nonCar + " agents where car is not the best transport mode.");
	}
	
	/*
	 * - Create 1000 persons per setup (age, gender). 
	 * - Calculate travel time on a dummy link.
	 * - Count number of agents faster/slower than reference agent.
	 */
	private static void calculateExpectedModeShare(Config config) {
		
		TravelTime travelTime = new WalkTravelTimeFactory(config.plansCalcRoute()).createTravelTime();
		Scenario sc = ScenarioUtils.createScenario(config);
		createNetwork(sc);
		
		NetworkFactory networkFactory = sc.getNetwork().getFactory();
		
		Node dummyNode1 = networkFactory.createNode(sc.createId("dummyNode1"), sc.createCoord(0.0, 0.0));
		Node dummyNode2 = networkFactory.createNode(sc.createId("dummyNode2"), sc.createCoord(100.0, 0.0));
		
		Link dummyLink = networkFactory.createLink(sc.createId("dummyLink"), dummyNode1, dummyNode2);
		dummyLink.setLength(1000.0);
		Set<String> modes = new HashSet<String>();
		modes.add(nonCarMode);
		dummyLink.setAllowedModes(modes);
		
		int numDraws = 1000;
		double[] travelTimes = new double[numDraws];
		
		for (int i = 0; i < numDraws; i++) {
			Id id = sc.createId(referenceSex + "_" + referenceAge + "_" + i);
			Person person = createPerson(sc, id, referenceSex, referenceAge, 0.0, nonCarMode);
			double tt = travelTime.getLinkTravelTime(dummyLink, 0.0, person, null);
			travelTimes[i] = tt;
		}
		
		double referenceTT = StatUtils.percentile(travelTimes, 50);
//		double referenceTT = StatUtils.mean(travelTimes);
		int car = 0;
		int nonCar = 0;
		
		String[] sexes = new String[]{"m", "f"};
		for (int age = 1; age <= 100; age++) {
			for (String sex : sexes) {
				travelTimes = new double[numDraws];
				for (int i = 0; i < numDraws; i++) {
					Id id = sc.createId(referenceSex + "_" + referenceAge + "_" + i);
					Person person = createPerson(sc, id, sex, age, 0.0, nonCarMode);
					double tt = travelTime.getLinkTravelTime(dummyLink, 0.0, person, null);
					travelTimes[i] = tt;
				}
				double tt = StatUtils.percentile(travelTimes, 50);
//				double tt = StatUtils.mean(travelTimes);
				if (tt > referenceTT) car++;
				else nonCar++;
			}
		}
		
		log.info("non-car reference travel time:\t" + referenceTT);
		log.info("expected car share:\t" + car/200.0);
		log.info("expected non-car share:\t"+ nonCar/200.0);
	}
	
	private static class MultiModalDemoControler extends Controler {

		public MultiModalDemoControler(Scenario scenario) {
			super(scenario);
			
			this.setScoringFunctionFactory(new OnlyTravelDependentScoringFunctionFactory());
			this.setTravelDisutilityFactory(new OnlyTimeDependentTravelCostCalculatorFactory());
		}
		
		@Override
		protected void setUp() {
			super.setUp();
			
			TravelTime bikeTravelTime = new BikeTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime();
			TravelTime walkTravelTime = new WalkTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime();
			
			super.getMultiModalTravelTimes().put(TransportMode.bike, bikeTravelTime);
			super.getMultiModalTravelTimes().put(TransportMode.walk, walkTravelTime);
		}
	}
	
	private static class TravelTimeAnalyzer implements AgentDepartureEventHandler, AgentArrivalEventHandler, IterationEndsListener {

		/*package*/ final Map<String, Map<Id, Double>> departures = new TreeMap<String, Map<Id, Double>>();
		/*package*/ final Map<String, Map<Id, Double>> arrivals = new TreeMap<String, Map<Id, Double>>();
		
		/*package*/ final Map<String, List<Double>> means = new TreeMap<String, List<Double>>();
		/*package*/ final Map<String, List<Double>> stds = new TreeMap<String, List<Double>>();
		/*package*/ final Map<String, List<Double>> medians = new TreeMap<String, List<Double>>();
		
		@Override
		public void reset(int iteration) {
			departures.clear();
			arrivals.clear();
		}

		@Override
		public void handleEvent(AgentDepartureEvent event) {
			Map<Id, Double> map = departures.get(event.getLegMode());
			if (map == null) {
				map = new HashMap<Id, Double>();
				departures.put(event.getLegMode(), map);
			}
			
			map.put(event.getPersonId(), event.getTime());
		}
		
		@Override
		public void handleEvent(AgentArrivalEvent event) {
			Map<Id, Double> map = arrivals.get(event.getLegMode());
			if (map == null) {
				map = new HashMap<Id, Double>();
				arrivals.put(event.getLegMode(), map);
			}
			
			map.put(event.getPersonId(), event.getTime());
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			
			if (event.getIteration() == 0) {
				for (String mode : CollectionUtils.stringToArray(legModes)) {
					means.put(mode, new ArrayList<Double>());
					stds.put(mode, new ArrayList<Double>());
					medians.put(mode, new ArrayList<Double>());
				}
			}
			
			for (String mode : arrivals.keySet()) {
				
				Map<Id, Double> modeDepartures = departures.get(mode);
				Map<Id, Double> modeArrivals = arrivals.get(mode);
				
				if (modeDepartures.size() != modeArrivals.size()) {
					throw new RuntimeException("Number of departures and arrivals on mode " + mode + " does not match!");
				}
				
				double[] travelTimes = new double[modeDepartures.size()];
				int i = 0;
				for (Entry<Id, Double> entry : modeArrivals.entrySet()) {
					double arrival = entry.getValue();
					double departure = modeDepartures.get(entry.getKey());
					travelTimes[i] = arrival - departure;
					i++;
				}
				
				// Compute statistics directly from the array
				// assume values is a double[] array
				double mean = StatUtils.mean(travelTimes);
				double std = Math.sqrt(StatUtils.variance(travelTimes, mean));
				double median = StatUtils.percentile(travelTimes, 50);
				
				means.get(mode).add(mean);
				stds.get(mode).add(std);
				medians.get(mode).add(median);
				
				log.info("Results for mode " + mode);
				log.info("\t" +"mean" + "\t" + mean);
				log.info("\t" +"std" + "\t" + std);
				log.info("\t" +"median" + "\t" + median);
				log.info("");
			}

			// If a mode was not used by any agent, set Double.NaN in the statistical data.
			for (String mode : CollectionUtils.stringToArray(legModes)) {
				if (!arrivals.keySet().contains(mode)) {
					means.get(mode).add(Double.NaN);
					stds.get(mode).add(Double.NaN);
					medians.get(mode).add(Double.NaN);
				}
			}
		}	
	}
}
