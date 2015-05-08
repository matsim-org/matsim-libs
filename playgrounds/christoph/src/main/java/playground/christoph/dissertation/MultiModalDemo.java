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

import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.analysis.christoph.ActivitiesAnalyzer;
import org.matsim.contrib.analysis.christoph.TravelTimesWriter;
import org.matsim.contrib.analysis.christoph.TripsAnalyzer;
import org.matsim.contrib.multimodal.ControlerDefaultsWithMultiModalModule;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.util.BikeTravelTimeFactory;
import org.matsim.contrib.multimodal.router.util.WalkTravelTimeFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.MultiNodeDijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

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
	
	private static int numIterations = 100;
	private static int numPersonsPerHour = 2500;
	private static int hours = 4;
	private static boolean createPlansForAllModes = false;
	private static String randomMode = "RANDOM";
	
	private static String initialLegMode = randomMode;
//	private static String initialLegMode = TransportMode.car;
//	private static String initialLegMode = TransportMode.walk;
	
	private static String nonCarMode = TransportMode.walk;
//	private static String legModes = TransportMode.car + "," + TransportMode.bike + "," + TransportMode.walk;
	/*package*/ static String legModes = TransportMode.car + "," + TransportMode.walk;
	
//	private static double capacity = Double.MAX_VALUE;
	static double capacity = 2500.0;

	private static double referenceCarSpeed = 50.0/3.6;
	
	private static String referenceSex = "m";
	private static int referenceAge = 50;
	
	/*
	 * If you have to adapt this, then something seems to be wrong! 
	 */
	private static final double expectedReferenceTravelTime = 1406.0;	// walk, m, 50
//	private static final double expectedReferenceTravelTime = 1408.0;	// walk, m, 50, no random term
	
	public static void main(String[] args) {
		
		// create and initialze config
		Config config = ConfigUtils.createConfig();
		
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setNumberOfThreads(1);
		qSimConfigGroup.setStartTime(0.0);
		qSimConfigGroup.setEndTime(3*86400.0);
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setRemoveStuckVehicles(false);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setVehicleBehavior(QSimConfigGroup.VEHICLE_BEHAVIOR_EXCEPTION);
		qSimConfigGroup.setStuckTime(25.0);
		
		config.travelTimeCalculator().setTraveltimeBinSize(300);
		config.travelTimeCalculator().setFilterModes(true);
		config.travelTimeCalculator().setAnalyzedModes("car");
		config.travelTimeCalculator().setTravelTimeGetterType("linearinterpolation");

        MultiModalConfigGroup multiModalConfigGroup = new MultiModalConfigGroup();
        config.addModule(multiModalConfigGroup);
        multiModalConfigGroup.setCreateMultiModalNetwork(false);
        multiModalConfigGroup.setDropNonCarRoutes(false);
        multiModalConfigGroup.setNumberOfThreads(1);
        multiModalConfigGroup.setMultiModalSimulationEnabled(true);
        multiModalConfigGroup.setSimulatedModes(TransportMode.bike + "," + TransportMode.walk);
		
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(numIterations);
		config.controler().setMobsim(ControlerConfigGroup.MobsimType.qsim.toString());
		config.controler().setOutputDirectory("../../matsim/mysimulations/dissertation/MultiModal/" 
				+ initialLegMode.toLowerCase() + "/" + "capacity_" + capacity);
		
		Set<String> networkRouteModes = CollectionUtils.stringToSet(TransportMode.car + "," + TransportMode.bike + "," + TransportMode.walk);
		config.plansCalcRoute().setNetworkModes(networkRouteModes);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, 1.34);
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.bike, 6.01);
		config.plansCalcRoute().setTeleportedModeFreespeedFactor(TransportMode.pt, 2.0);
		
		config.strategy().setMaxAgentPlanMemorySize(4);
		config.strategy().addParam("Module_1", "SelectExpBeta");
//		config.strategy().addParam("Module_1", "BestScore");
		config.strategy().addParam("ModuleProbability_1", "0.90");
		config.strategy().addParam("Module_2", "playground.christoph.dissertation.ChooseBestLegModePlanStrategy");
		config.strategy().addParam("ModuleProbability_2", "0.10");
//		config.strategy().addParam("ModuleDisableAfterIteration_2", String.valueOf(numIterations - 1));
		
		
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(24*3600);
		config.planCalcScore().addActivityParams(home);
		
		ConfigGroup module = config.createModule("changeLegMode");
		module.addParam("modes", legModes);	
			
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		createPopulation(scenario);
		
		adaptNetwork(scenario);
		
//		Controler controler = new MultiModalDemoControler(scenario);
		Controler controler = null ;
		controler.setOverwriteFiles(true);
		
		// Multi-modal simulation
        controler.setModules(new ControlerDefaultsWithMultiModalModule());

        // TravelTimeAnalyzer
		TravelTimeAnalyzer travelTimeAnalyzer = new TravelTimeAnalyzer(scenario);
		controler.getEvents().addHandler(travelTimeAnalyzer);
		controler.addControlerListener(travelTimeAnalyzer);
		
		// TripsAnalyzer
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				/*
				 * Create average travel time statistics.
				 */
				String tripsFileName = "tripCounts";
				String durationsFileName = "tripDurations";
				String outputTripsFileName = event.getControler().getControlerIO().getOutputFilename(tripsFileName);
				String outputDurationsFileName = event.getControler().getControlerIO().getOutputFilename(durationsFileName);
				Set<String> modes = new HashSet<String>();
				modes.add(TransportMode.bike);
				modes.add(TransportMode.car);
				modes.add(TransportMode.pt);
				modes.add(TransportMode.ride);
				modes.add(TransportMode.walk);
				
				// create TripsAnalyzer and register it as ControlerListener and EventsHandler
				TripsAnalyzer tripsAnalyzer = new TripsAnalyzer(outputTripsFileName, outputDurationsFileName, modes, true);
				event.getControler().addControlerListener(tripsAnalyzer);
				event.getControler().getEvents().addHandler(tripsAnalyzer);
				
				// TripsAnalyzer is a StartupEventListener, therefore pass event over to it.
				tripsAnalyzer.notifyStartup(event);
				
				// create ActivitiesAnalyzer and register it as ControlerListener and EventsHandler
				String activitiesFileName = "activityCounts";
				Set<String> activityTypes = new TreeSet<String>(event.getControler().getConfig().planCalcScore().getActivityTypes());
				ActivitiesAnalyzer activitiesAnalyzer = new ActivitiesAnalyzer(activitiesFileName, activityTypes, true);
				event.getControler().addControlerListener(activitiesAnalyzer);
				event.getControler().getEvents().addHandler(activitiesAnalyzer);
			}
		});
		
		// car travel times writer
		TravelTimesWriter travelTimesWriter = new TravelTimesWriter(true, false);
		controler.addControlerListener(travelTimesWriter);
				
		controler.run();
		
		calculatePopulationStatistics(scenario);
		
		calculateExpectedModeShare(config);
	}

	private static void createNetwork(Scenario scenario) {
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		
		Node n1 = networkFactory.createNode(Id.create("n1", Node.class), scenario.createCoord(0.0, 0.0));
		Node n2 = networkFactory.createNode(Id.create("n2", Node.class), scenario.createCoord(100.0, 0.0));
		Node n3 = networkFactory.createNode(Id.create("n3", Node.class), scenario.createCoord(100.0, 1000.0));
		Node n4 = networkFactory.createNode(Id.create("n4", Node.class), scenario.createCoord(2100.0, 1000.0));
		Node n5 = networkFactory.createNode(Id.create("n5", Node.class), scenario.createCoord(2100.0, 0.0));
		Node n6 = networkFactory.createNode(Id.create("n6", Node.class), scenario.createCoord(2200.0, 0.0));
		
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
	
		Link l1 = networkFactory.createLink(Id.create("l1", Link.class), n1, n2);
		l1.setLength(100.0);
//		l1.setCapacity(capacity);
		/*
		 * Avoid that vehicles cannot be moved from the waiting queue onto the link.
		 * Time they spent in the waiting queue is not noticed by the TravelTimeCalculator.
		 */
		l1.setCapacity(Double.MAX_VALUE);	
		l1.setFreespeed(referenceCarSpeed);
		l1.setAllowedModes(multiMode);
		
		Link l2 = networkFactory.createLink(Id.create("l2", Link.class), n2, n3);
		l2.setLength(1000.0);
//		l2.setCapacity(capacity);
		l2.setCapacity(Double.MAX_VALUE);
		l2.setFreespeed(referenceCarSpeed);
		l2.setAllowedModes(carMode);
		
		Link l3 = networkFactory.createLink(Id.create("l3", Link.class), n3, n4);
//		l3.setLength(1000.0);
		l3.setLength(2000.0);
		l3.setCapacity(capacity);
		l3.setFreespeed(referenceCarSpeed);
		l3.setAllowedModes(carMode);

		Link l4 = networkFactory.createLink(Id.create("l4", Link.class), n4, n5);
		l4.setLength(1000.0);
//		l4.setCapacity(capacity);
		l4.setCapacity(Double.MAX_VALUE);
		l4.setFreespeed(referenceCarSpeed);
		l4.setAllowedModes(carMode);

		Link l5 = networkFactory.createLink(Id.create("l5", Link.class), n2, n5);
//		l5.setLength(1000.0);
		l5.setLength(2000.0);
		l5.setCapacity(capacity);
		l5.setFreespeed(referenceCarSpeed);
		l5.setAllowedModes(nonCarMode);
		
		Link l6 = networkFactory.createLink(Id.create("l6", Link.class), n5, n6);
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
		Link l3 = scenario.getNetwork().getLinks().get(Id.create("l3", Link.class));
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
		Link l2 = scenario.getNetwork().getLinks().get(Id.create("l2", Link.class));
		Link l4 = scenario.getNetwork().getLinks().get(Id.create("l4", Link.class));
		Link l6 = scenario.getNetwork().getLinks().get(Id.create("l6", Link.class));
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
		Person person = createPerson(sc, Id.create("1", Person.class), referenceSex, referenceAge);
		Plan plan = createPlan(sc, 0.0, TransportMode.car);
		person.addPlan(plan);
		sc.getPopulation().addPerson(person);
		
		// create routes
//		Controler controler = new MultiModalDemoControler(sc);
		Controler controler = null ;
		controler.setOverwriteFiles(true);
		
		// Multi-modal simulation
        controler.setModules(new ControlerDefaultsWithMultiModalModule());

        TravelTimeAnalyzer travelTimeAnalyzer = new TravelTimeAnalyzer(sc);
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
			Person person = createPerson(sc, Id.create(String.valueOf(i), Person.class), referenceSex, referenceAge);
			Plan plan = createPlan(sc, 0.0, nonCarMode);
			person.addPlan(plan);
			sc.getPopulation().addPerson(person);
		}
		
		// create routes
//		Controler controler = new MultiModalDemoControler(sc);
		Controler controler = null ;
		controler.setOverwriteFiles(true);
		
		// Multi-modal simulation
        controler.setModules(new ControlerDefaultsWithMultiModalModule());

        TravelTimeAnalyzer travelTimeAnalyzer = new TravelTimeAnalyzer(sc);
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
//		MatsimRandom.reset();
		Random random = MatsimRandom.getLocalInstance();
		
		// Draw some more random numbers to get a better 50:50 share if the initial mode is chosen randomly.
		for (int i = 0; i < 2046; i++) random.nextInt();
		
		Map<Id, Double> departureTimes = new HashMap<Id, Double>();
		
		for (int hour = 0; hour < hours; hour++) {
			
			for (int i = 0; i < numPersonsPerHour; i++) {
				
				String sex;
				if (random.nextDouble() > 0.5) sex = "f";
				else sex = "m";
				
				int age = 18 + random.nextInt(82);
				
				Id<Person> personId = Id.create(String.valueOf(hour * numPersonsPerHour + i), Person.class);
				
				double departureTime = 8*3600 + hour*3600 + random.nextInt(3600);
				departureTimes.put(personId, departureTime);
				
				Person person = createPerson(scenario, personId, sex, age);
		
				scenario.getPopulation().addPerson(person);
			}
		}
		
		// create and add plans
		for (Person person : scenario.getPopulation().getPersons().values()) {
			double departureTime = departureTimes.get(person.getId());
			if (createPlansForAllModes) {
				for (String legMode : CollectionUtils.stringToArray(legModes)) {					
					Plan plan = createPlan(scenario, departureTime, legMode);
					person.addPlan(plan);
					if (legMode.equals(initialLegMode)) ((PersonImpl) person).setSelectedPlan(plan);
				}
				if (initialLegMode.equals(randomMode)) ((PersonImpl) person).setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan((person)));				
			} else {
				if (initialLegMode.equals(randomMode)) {
					String[] modes = CollectionUtils.stringToArray(legModes);
					Plan plan = createPlan(scenario, departureTime, modes[random.nextInt(modes.length)]);
					person.addPlan(plan);
				} else {
					Plan plan = createPlan(scenario, departureTime, initialLegMode);
					person.addPlan(plan);
				}
			}
		}
	}
	
	private static Person createPerson(Scenario scenario, Id<Person> id, String sex, int age) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		Person person = populationFactory.createPerson(id);
		
		((PersonImpl) person).setSex(sex);
		((PersonImpl) person).setAge(age);
		((PersonImpl) person).setCarAvail("always");
		
		return person;
	}
	
	private static Plan createPlan(Scenario scenario, double departureTime, String legMode) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
				
		Plan plan = populationFactory.createPlan();
		
		Activity a1 = populationFactory.createActivityFromLinkId("home", Id.create("l1", Link.class));
		((ActivityImpl) a1).setCoord(scenario.getNetwork().getLinks().get(Id.create("l1", Link.class)).getCoord());
		a1.setEndTime(departureTime);
		
		Leg l1 = populationFactory.createLeg(legMode);
		l1.setDepartureTime(departureTime);
		
		Activity a2 = populationFactory.createActivityFromLinkId("home", Id.create("l6", Link.class));
		((ActivityImpl) a2).setCoord(scenario.getNetwork().getLinks().get(Id.create("l6", Link.class)).getCoord());
		
		plan.addActivity(a1);
		plan.addLeg(l1);
		plan.addActivity(a2);
		
		return plan;
	}
	
	private static void calculatePopulationStatistics(Scenario scenario) {
		
		int car = 0;
		int nonCar = 0;
		
		double[] ages = new double[numPersonsPerHour * hours];
		int males = 0;
		int females = 0;
		
		int i = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			ages[i] = ((PersonImpl) person).getAge();
			if(((PersonImpl) person).getSex().equals("m")) males++;
			else females++;
			
			Leg leg = (Leg) new BestPlanSelector<Plan, Person>().selectPlan((person)).getPlanElements().get(1);
			if (leg.getMode().equals(TransportMode.car)) car++;
			else nonCar++;
			
			i++;
		}
		
		log.info("Found " + males + " male agents (" + (100.0*males)/(numPersonsPerHour * hours) + "%).");
		log.info("Found " + females + " female agents (" + (100.0*females)/(numPersonsPerHour * hours) + "%).");
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
		
		TravelTime travelTime = new WalkTravelTimeFactory(config.plansCalcRoute()).get();
		Scenario sc = ScenarioUtils.createScenario(config);
		createNetwork(sc);
		
		NetworkFactory networkFactory = sc.getNetwork().getFactory();
		
		Node dummyNode1 = networkFactory.createNode(Id.create("dummyNode1", Node.class), sc.createCoord(0.0, 0.0));
		Node dummyNode2 = networkFactory.createNode(Id.create("dummyNode2", Node.class), sc.createCoord(100.0, 0.0));
		
		Link dummyLink = networkFactory.createLink(Id.create("dummyLink", Link.class), dummyNode1, dummyNode2);
		dummyLink.setLength(1000.0);
		Set<String> modes = new HashSet<String>();
		modes.add(nonCarMode);
		dummyLink.setAllowedModes(modes);
		
		int numDraws = 1000;
		double[] travelTimes = new double[numDraws];
		
		for (int i = 0; i < numDraws; i++) {
			Id<Person> id = Id.create(referenceSex + "_" + referenceAge + "_" + i, Person.class);
			Person person = createPerson(sc, id, referenceSex, referenceAge);
			Plan plan = createPlan(sc, 0.0, nonCarMode);
			person.addPlan(plan);
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
					Id<Person> id = Id.create(referenceSex + "_" + referenceAge + "_" + i, Person.class);
					Person person = createPerson(sc, id, sex, age);
					Plan plan = createPlan(sc, 0.0, nonCarMode);
					person.addPlan(plan);
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
	
	private static class MultiModalDemoControler{ 
//	extends Controler {

		public MultiModalDemoControler(Scenario scenario) {
//			super(scenario);
//			
//			this.setScoringFunctionFactory(new OnlyTravelTimeDependentScoringFunctionFactory());
//			this.addOverridingModule(new AbstractModule() {
//				@Override
//				public void install() {
//					bindTravelDisutilityFactory().toInstance(new OnlyTimeDependentTravelDisutilityFactory());
//				}
//			});
			
			throw new RuntimeException( Gbl.SET_UP_IS_NOW_FINAL + Gbl.CONTROLER_IS_NOW_FINAL ) ;
		}
		
//		@Override
//		protected void setUp() {
//			super.setUp();
//			
//			TravelTime carTravelTime = this.getLinkTravelTimes();
//			TravelTime bikeTravelTime = new BikeTravelTimeFactory(this.getConfig().plansCalcRoute()).get();
//			TravelTime walkTravelTime = new WalkTravelTimeFactory(this.getConfig().plansCalcRoute()).get();
//			
//			int timeSlice = this.getConfig().travelTimeCalculator().getTraveltimeBinSize();
//			int maxTime = 30 * 3600;
//			WaitToLinkCalculator waitToLinkCalculator = new WaitToLinkCalculator(timeSlice, maxTime);
//			this.getEvents().addHandler(waitToLinkCalculator);
//			this.addControlerListener(waitToLinkCalculator);
//			
//			Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
//			
//			travelTimes.put(TransportMode.car, carTravelTime);
//			travelTimes.put(TransportMode.bike, bikeTravelTime);
//			travelTimes.put(TransportMode.walk, walkTravelTime);
//			
//			for (GenericPlanStrategy<Plan, Person> planStrategy : this.getStrategyManager().getStrategiesOfDefaultSubpopulation()) {
//				if (planStrategy instanceof ChooseBestLegModePlanStrategy) {
//					((ChooseBestLegModePlanStrategy) planStrategy).setWaitToLinkCalculator(waitToLinkCalculator);
//					((ChooseBestLegModePlanStrategy) planStrategy).setTravelTimes(travelTimes);
//					((ChooseBestLegModePlanStrategy) planStrategy).setTravelDisutilityFactory(this.getTravelDisutilityFactory());
//				}
//			}
//		}
	}
	
	private static class TravelTimeAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler, IterationEndsListener {

		/*package*/ final Scenario scenario;
		
		/*package*/ final Map<String, Map<Id, Double>> departures = new TreeMap<String, Map<Id, Double>>();
		/*package*/ final Map<String, Map<Id, Double>> arrivals = new TreeMap<String, Map<Id, Double>>();
		/*package*/ final Map<Id, String> modes = new HashMap<Id, String>();
		
		/*package*/ final Map<String, List<Double>> means = new TreeMap<String, List<Double>>();
		/*package*/ final Map<String, List<Double>> stds = new TreeMap<String, List<Double>>();
		/*package*/ final Map<String, List<Double>> medians = new TreeMap<String, List<Double>>();
		
		public TravelTimeAnalyzer(Scenario scenario) {
			this.scenario = scenario;
		}
		
		@Override
		public void reset(int iteration) {
			departures.clear();
			arrivals.clear();
			modes.clear();
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			Map<Id, Double> map = departures.get(event.getLegMode());
			if (map == null) {
				map = new HashMap<Id, Double>();
				departures.put(event.getLegMode(), map);
			}
			
			map.put(event.getPersonId(), event.getTime());
			modes.put(event.getPersonId(), event.getLegMode());
		}
		
		@Override
		public void handleEvent(PersonArrivalEvent event) {
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
			
			try {
				OutputDirectoryHierarchy outputDirectoryHierarchy = event.getControler().getControlerIO();
				String travelTimesFileName = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "travelTimes.txt");
				BufferedWriter writer = IOUtils.getBufferedWriter(travelTimesFileName);
				
				writer.write("Id");
				writer.write("\t");
				writer.write("age");
				writer.write("\t");
				writer.write("gender");
				writer.write("\t");
				writer.write("mode");
				writer.write("\t");
				writer.write("departure");
				writer.write("\t");
				writer.write("arrival");
				writer.write("\t");
				writer.write("traveltime");
				writer.write("\n");
				
				for (Person person : scenario.getPopulation().getPersons().values()) {
					String mode = modes.get(person.getId());
					double departure = this.departures.get(mode).get(person.getId());
					double arrival = this.arrivals.get(mode).get(person.getId());
					
					writer.write(person.getId().toString());
					writer.write("\t");
					writer.write(String.valueOf(((PersonImpl) person).getAge()));
					writer.write("\t");
					writer.write(((PersonImpl) person).getSex());
					writer.write("\t");
					writer.write(mode);
					writer.write("\t");
					writer.write(String.valueOf(departure));
					writer.write("\t");
					writer.write(String.valueOf(arrival));
					writer.write("\t");
					writer.write(String.valueOf(arrival - departure));
					writer.write("\n");
				}
				
				writer.flush();
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}			
	}
	
	/*package*/ static class ChooseBestLegModeModule extends AbstractMultithreadedModule {
		
		private final Scenario scenario;
		private final Set<String> modes;
		private Map<String, TravelTime> travelTimes;
		private WaitToLinkCalculator waitToLinkCalculator;
		private TravelDisutilityFactory travelDisutilityFactory;
		
		public ChooseBestLegModeModule(final Scenario scenario, final Set<String> modes) {
			super(scenario.getConfig().global());
			this.scenario = scenario;
			this.modes = modes;
		}

		public void setTravelTimes(Map<String, TravelTime> travelTimes) {
			this.travelTimes = travelTimes;
		}
		
		public void setWaitToLinkCalculator(WaitToLinkCalculator waitToLinkCalculator) {
			this.waitToLinkCalculator = waitToLinkCalculator;
		}
		
		public void setTravelDisutilityFactory(TravelDisutilityFactory travelDisutilityFactory) {
			this.travelDisutilityFactory = travelDisutilityFactory;
		}
		
		@Override
		public PlanAlgorithm getPlanAlgoInstance() {
				
			Map<String, LeastCostPathCalculator> leastCostPathCalculators = new HashMap<String, LeastCostPathCalculator>();
			for (String mode : travelTimes.keySet()) {				
				TravelTime travelTime = this.travelTimes.get(mode);
				TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime, 
						scenario.getConfig().planCalcScore());
				
				LeastCostPathCalculator leastCostPathCalculator = new MultiNodeDijkstraFactory().createPathCalculator(scenario.getNetwork(), 
						travelDisutility, travelTime);
				((IntermodalLeastCostPathCalculator) leastCostPathCalculator).setModeRestriction(CollectionUtils.stringToSet(mode));
				
				leastCostPathCalculators.put(mode, leastCostPathCalculator);
			}
			
			return new ChooseBestLegMode(leastCostPathCalculators, waitToLinkCalculator, scenario.getNetwork(), modes);
		}
	}
	
	public static class ChooseBestLegMode implements PlanAlgorithm {

		private final Map<String, LeastCostPathCalculator> leastCostPathCalculators;
		private final WaitToLinkCalculator waitToLinkCalculator;
		private final Network network;
		private final Set<String> modes;
		private final Random random;
		
		private ChooseBestLegMode(Map<String, LeastCostPathCalculator> leastCostPathCalculators, 
				WaitToLinkCalculator waitToLinkCalculator, Network network, final Set<String> modes) {
			this.leastCostPathCalculators = leastCostPathCalculators;
			this.waitToLinkCalculator = waitToLinkCalculator;
			this.network = network;
			this.modes = modes;
			this.random = MatsimRandom.getLocalInstance();
		}
		
		@Override
		public void run(Plan plan) {
			
			String bestMode = null;
			double minTravelTime = Double.MAX_VALUE;
			
			for (String mode : modes) {
				double travelTime = 0.0;
				
				LeastCostPathCalculator leastCostPathCalculator = leastCostPathCalculators.get(mode);
				
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						
						Link fromLink = network.getLinks().get(leg.getRoute().getStartLinkId());
						Link toLink = network.getLinks().get(leg.getRoute().getEndLinkId());
						
						double waitToLinkTime = 1.0;	// at least one second due to simulation logic
						
						// if it is car mode also take waitToLink time into account
						if (mode.equals(TransportMode.car)) {
							waitToLinkTime = waitToLinkCalculator.getWaitToLinkTime(leg.getRoute().getStartLinkId(), leg.getDepartureTime()); 
						}
						travelTime += waitToLinkTime;
						
						Path path = leastCostPathCalculator.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), 
								leg.getDepartureTime() + waitToLinkTime, plan.getPerson(), null);
						travelTime += path.travelTime;
						
						// add also costs of to-link
						path = leastCostPathCalculator.calcLeastCostPath(toLink.getFromNode(), toLink.getToNode(), 
								leg.getDepartureTime() + path.travelTime + waitToLinkTime, plan.getPerson(), null);
						travelTime += path.travelTime;
												
						if (travelTime < minTravelTime) {
							bestMode = mode;
							minTravelTime = travelTime;
						} else if (travelTime == minTravelTime) {
							if (this.random.nextBoolean()) {
								bestMode = mode;
								minTravelTime = travelTime;
							}
						}
					}
				}
			}
			
			// set mode
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					leg.setMode(bestMode);
				}
			}
		}
		
	}
}