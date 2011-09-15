/* *********************************************************************** *
 * project: org.matsim.*
 * Simulator.java
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
package playground.johannes.studies.coopsim;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.coopsim.LoggerUtils;
import playground.johannes.coopsim.SimEngine;
import playground.johannes.coopsim.analysis.ActivityDurationTask;
import playground.johannes.coopsim.analysis.ArrivalTimeTask;
import playground.johannes.coopsim.analysis.JointActivityTask;
import playground.johannes.coopsim.analysis.PlansWriterTask;
import playground.johannes.coopsim.analysis.ScoreTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripDistanceTask;
import playground.johannes.coopsim.eval.EvalEngine;
import playground.johannes.coopsim.mental.MentalEngine;
import playground.johannes.coopsim.mental.choice.ActivityFacilitySelector;
import playground.johannes.coopsim.mental.choice.ActivityGroupSelector;
import playground.johannes.coopsim.mental.choice.ActivityTypeSelector;
import playground.johannes.coopsim.mental.choice.ArrivalTimeSelector;
import playground.johannes.coopsim.mental.choice.ChoiceSelector;
import playground.johannes.coopsim.mental.choice.ChoiceSelectorComposite;
import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.coopsim.mental.choice.DurationSelector;
import playground.johannes.coopsim.mental.choice.EgoSelector;
import playground.johannes.coopsim.mental.choice.EgosFacilities;
import playground.johannes.coopsim.mental.choice.EgosHome;
import playground.johannes.coopsim.mental.choice.PlanIndexSelector;
import playground.johannes.coopsim.mental.choice.RandomAlter;
import playground.johannes.coopsim.mental.planmod.ActivityDurationModAdaptor;
import playground.johannes.coopsim.mental.planmod.ActivityFacilityModAdaptor;
import playground.johannes.coopsim.mental.planmod.ActivityTypeModAdaptor;
import playground.johannes.coopsim.mental.planmod.ArrivalTimeModAdaptor;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptorComposite;
import playground.johannes.coopsim.pysical.PhysicalEngine;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.statistics.ExponentialDistribution;
import playground.johannes.socialnetworks.statistics.LogNormalDistribution;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author illenberger
 *
 */
public class Simulator {
	
	private static final Logger logger = Logger.getLogger(Simulator.class);

	private static SocialGraph graph;
	
	private static NetworkImpl network;
	
	private static ActivityFacilitiesImpl facilities;
	
	private static Config config;
	
	public static void main(String[] args) throws IOException {
		LoggerUtils.setDisallowVerbose(false);
		
		LoggerUtils.setVerbose(false);
		config = new Config();
		config.addCoreModules();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		LoggerUtils.setVerbose(true);
		
		loadData(config);

		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
		int iterations = (int) Double.parseDouble(config.getParam("controler", "lastIteration"));
		double beta = Double.parseDouble(config.getParam("socialnets", "beta_join"));
		String output = config.getParam("controler", "outputDirectory");
		int sampleInterval = (int) Double.parseDouble(config.getParam("socialnets", "sampleinterval"));
		/*
		 * initialize physical engine
		 */
		logger.info("Initializing physical engine...");
		PhysicalEngine physical = new PhysicalEngine(network);
		/*
		 * do some pre-processing
		 */
		logger.info("Validating facilities...");
		FacilityValidator.generate(facilities, network, graph);
		
		logger.info("Generation initial state...");
		NetworkLegRouter router = initRouter(physical.getTravelTime());
		InitialStateGenerator.generate(graph, facilities, router);
		/*
		 * initialize choice selectors
		 */
		logger.info("Initializing choice selectors...");
		ChoiceSelector choiceSelector = initSelectors(random);
		/*
		 * initialize adaptors
		 */
		logger.info("Initializing choice adaptors...");
		Choice2ModAdaptorComposite adaptor = new Choice2ModAdaptorComposite();
		adaptor.addComponent(new ActivityTypeModAdaptor());
		adaptor.addComponent(new ActivityFacilityModAdaptor(facilities, router));
		adaptor.addComponent(new ArrivalTimeModAdaptor());
		adaptor.addComponent(new ActivityDurationModAdaptor());
		/*
		 * initialize mental engine
		 */
		logger.info("Initializing mental engine...");
		MentalEngine mental = new MentalEngine(graph, choiceSelector, adaptor, random);
		/*
		 * initialize scoring
		 */
		logger.info("Initializing evaluation engine...");
		LoggerUtils.setVerbose(false);
		EvalEngine eval = new EvalEngine(graph, physical.getVisitorTracker(), (PlanCalcScoreConfigGroup) config.getModule("planCalcScore"), beta);
		LoggerUtils.setVerbose(true);
		/*
		 * initialize simulation engine
		 */
		logger.info("Initializing simulation engine...");
		SimEngine simEngine = new SimEngine(graph, mental, physical, eval);
		simEngine.setSampleInterval(sampleInterval);
		simEngine.setLogInerval(500);
		/*
		 * initialize analyzer tasks
		 */
		TrajectoryAnalyzerTask task = initAnalyzerTask(physical, eval);
		simEngine.setAnalyzerTask(task, output);
		
		simEngine.run(iterations);
	}

	private static void loadData(Config config) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		logger.info("Loading network data...");
		LoggerUtils.setVerbose(false);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		network = scenario.getNetwork();
		LoggerUtils.setVerbose(true);
		
		logger.info("Loading facilities data...");
		LoggerUtils.setVerbose(false);
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(config.getParam("facilities", "inputFacilitiesFile"));
		facilities = scenario.getActivityFacilities();
		LoggerUtils.setVerbose(true);
		
		logger.info("Loading social graph data...");
		LoggerUtils.setVerbose(false);
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		graph = reader.readGraph(config.getParam("socialnets", "graphfile"));
		LoggerUtils.setVerbose(true);
	}
	
	private static NetworkLegRouter initRouter(final TravelTime travelTime) {
		TravelCost travelCost = new TravelCost() {
			@Override
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return travelTime.getLinkTravelTime(link, time);
			}
		};
		
		LeastCostPathCalculator router = new Dijkstra(network, travelCost, travelTime);
		NetworkLegRouter legRouter = new NetworkLegRouter(network, router, new ModeRouteFactory());
		
		return legRouter;
	}
	
	private static ChoiceSelector initSelectors(Random random) throws IOException {
		ChoiceSelectorComposite choiceSelector = new ChoiceSelectorComposite();
		/*
		 * initialize plan index selector
		 */
		choiceSelector.addComponent(new PlanIndexSelector());
		/*
		 * initialize ego selector
		 */
		choiceSelector.addComponent(new EgoSelector(graph, random));
		/*
		 * initialize activity type selector
		 */
		ChoiceSet<String> actTypeChoiceSet = new ChoiceSet<String>(random);
//		actTypeChoiceSet.addChoice("leisure", 1.0);
		actTypeChoiceSet.addChoice("visit", 36);
		actTypeChoiceSet.addChoice("gastro", 18);
		actTypeChoiceSet.addChoice("culture", 46);
		ActivityTypeSelector actTypeSelector = new ActivityTypeSelector(actTypeChoiceSet);
		choiceSelector.addComponent(actTypeSelector);
		/*
		 * initialize group selector
		 */
		ActivityGroupSelector groupSelector = new ActivityGroupSelector();
//		groupSelector.addGenerator("leisure", new RandomAlter(random));
		groupSelector.addGenerator("visit", new RandomAlter(random));
		groupSelector.addGenerator("gastro", new RandomAlter(random));
		groupSelector.addGenerator("culture", new RandomAlter(random));
		choiceSelector.addComponent(groupSelector);
		/*
		 * initialize facility selector
		 */
		ActivityFacilitySelector facilitySelector = new ActivityFacilitySelector();
//		facilitySelector.addGenerator("leisure", new EgosHome(random));
		facilitySelector.addGenerator("visit", new EgosHome(random));
//		FacilityChoiceSetGenerator generator = new FacilityChoiceSetGenerator(-1.4, 5, random, CartesianDistanceCalculator.getInstance());
//		try {
//			generator.write(generator.generate(graph, facilities, "gastro"), "/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.gastro.txt");
//			generator.write(generator.generate(graph, facilities, "culture"), "/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.culture.txt");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.exit(0);
		facilitySelector.addGenerator("gastro", new EgosFacilities(FacilityChoiceSetGenerator.read(config.getParam("socialnets", "choiceset_gastro"), graph), random));
		facilitySelector.addGenerator("culture", new EgosFacilities(FacilityChoiceSetGenerator.read(config.getParam("socialnets", "choiceset_culture"), graph), random));
		choiceSelector.addComponent(facilitySelector);
		/*
		 * initialize arrival time selector
		 */
		UnivariateRealFunction pdf = new LogNormalDistribution(0.3, 10.8, 1162);
		Map<SocialVertex, Double> times = initTimes(pdf, 28800, 86400, random);
		choiceSelector.addComponent(new ArrivalTimeSelector(times, random));
		/*
		 * initialize duration selector
		 */
		pdf = new ExponentialDistribution(-0.0001, 0.06);
		times = initTimes(pdf, 0, 43200, random);
		choiceSelector.addComponent(new DurationSelector(times, random));
		
		
		return choiceSelector;
	}
	
	private static Map<SocialVertex, Double> initTimes(UnivariateRealFunction pdf, int min, int max, Random random) {
		TimeSampler sampler = new TimeSampler(pdf, max, random);
		Map<SocialVertex, Double> map = new HashMap<SocialVertex, Double>(graph.getVertices().size());
//		BufferedWriter writer;
//		try {
//			writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/socialnets/locationChoice/output/samples.txt"));
//		ProgressLogger.init(graph.getVertices().size(), 1, 5);
		
		for(SocialVertex v : graph.getVertices()) {
			double sample =sampler.nextSample();
//			writer.write(String.valueOf(sample));
//			writer.newLine();
			map.put(v, sample);
//			ProgressLogger.step();
		}
//		writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return map;
	}
	
	private static TrajectoryAnalyzerTask initAnalyzerTask(PhysicalEngine physical, EvalEngine eval) {
		TrajectoryAnalyzerTaskComposite composite = new TrajectoryAnalyzerTaskComposite();
		composite.addTask(new PlansWriterTask(network));
		composite.addTask(new ArrivalTimeTask());
		composite.addTask(new ActivityDurationTask());
		composite.addTask(new TripDistanceTask(facilities));
//		composite.addTask(new TripDistanceAccessibilityTask(graph, facilities));
		composite.addTask(new JointActivityTask(graph, physical.getVisitorTracker()));
		composite.addTask(new ScoreTask(eval));
		return composite;
	}
}
