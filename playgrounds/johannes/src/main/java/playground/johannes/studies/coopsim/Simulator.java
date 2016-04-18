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

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.collections.ChoiceSet;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.util.LoggerUtils;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.sna.math.GaussDistribution;
import org.matsim.contrib.socnetgen.sna.math.LinearDistribution;
import org.matsim.contrib.socnetgen.sna.math.LogNormalDistribution;
import org.matsim.contrib.socnetgen.sna.math.PowerLawDistribution;
import org.matsim.contrib.socnetgen.sna.util.MultiThreading;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.vehicles.Vehicle;
import playground.johannes.coopsim.Profiler;
import playground.johannes.coopsim.SimEngine;
import playground.johannes.coopsim.analysis.*;
import playground.johannes.coopsim.eval.*;
import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.mental.MentalEngine;
import playground.johannes.coopsim.mental.choice.*;
import playground.johannes.coopsim.mental.planmod.*;
import playground.johannes.coopsim.mental.planmod.concurrent.ConcurrentPlanModEngine;
import playground.johannes.coopsim.pysical.PhysicalEngine;
import playground.johannes.coopsim.utils.NetworkLegRouter;
import playground.johannes.studies.sbsurvey.io.SocialSparseGraphMLReader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * @author illenberger
 *
 */
public class Simulator {
	
	private static final Logger logger = Logger.getLogger(Simulator.class);
	
	private static final String SOCNET_MODULE_NAME = "socialnets";

	private static SocialGraph graph;
	
	private static NetworkImpl network;
	
	private static ActivityFacilities facilities;
	
	private static Config config;
	
	private static Map<Person, ActivityDesires> personDesires;
	
	private static Map<SocialVertex, ActivityDesires> vertexDesires;
	
	private static ScoreTask scoreTask;
	
	private static boolean nonCooperativeMode = false;
	
	public static void main(String[] args) throws IOException {
		LoggerUtils.setDisableVerbose(false);
		
		LoggerUtils.setVerbose(false);
		config = new Config();
		config.addCoreModules();
		ConfigReader creader = new ConfigReader(config);
		creader.readFile(args[0]);
		LoggerUtils.setVerbose(true);
		
		loadData(config);

		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
		int iterations = (int) Double.parseDouble(config.getParam("controler", "lastIteration"));
		String output = config.getParam("controler", "outputDirectory");
		int sampleInterval = (int) Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "sampleinterval"));
		/*
		 * initialize physical engine
		 */
		logger.info("Initializing physical engine...");
		String strval = config.findParam(SOCNET_MODULE_NAME, "ttFactor");
		double ttFactor = 1.0;
		if(strval != null)
			ttFactor = Double.parseDouble(strval);
		PhysicalEngine physical = new PhysicalEngine(network, ttFactor);
		/*
		 * do some pre-processing
		 */
		logger.info("Validating facilities...");
		FacilityValidator.generate(facilities, network, graph);
		
		String plansFile = config.findParam("plans", "inputPlansFile");
		if(plansFile == null) {
			logger.info("Generating initial state...");
			NetworkLegRouter router = initRouter(physical.getTravelTime());
			InitialStateGenerator.generate(graph, facilities, router);
		} else {
			logger.info("Loading initial state...");
			loadPlans(plansFile);
		}
		/*
		 * load desires
		 */
		logger.info("Initializing desires...");
		loadDesires(config.findParam(SOCNET_MODULE_NAME, "desirefile"), random);
		/*
		 * initialize choice selectors
		 */
		logger.info("Initializing choice selectors...");
		if(config.getParam("socialnets", "noncooperative") != null) {
			nonCooperativeMode = Boolean.parseBoolean(config.getParam(SOCNET_MODULE_NAME, "noncooperative"));
			if(nonCooperativeMode)
				logger.warn("*\n*** Simulation runs in non-cooperative mode! ***\n*");
		}
		ChoiceSelector choiceSelector = initSelectors(random);
		/*
		 * initialize adaptors
		 */
		logger.info("Initializing choice adaptors...");
		AdaptorFactory adaptorFactory = new AdaptorFactory(physical);
		/*
		 * initialize mental engine
		 */
		logger.info("Initializing mental engine...");
		
		boolean includeAlters = Boolean.parseBoolean(config.getParam(SOCNET_MODULE_NAME, "includeAlters"));
		int threads = config.global().getNumberOfThreads();
		MultiThreading.setNumAllowedThreads(threads);
		MentalEngine mental;
		if(threads > 1)
			mental = new MentalEngine(graph, choiceSelector, new ConcurrentPlanModEngine(adaptorFactory), random, includeAlters);
		else
			mental = new MentalEngine(graph, choiceSelector, adaptorFactory.create(), random, includeAlters);
		/*
		 * initialize scoring
		 */
		logger.info("Initializing evaluation engine...");
		Map<String, Double> priorities = new HashMap<String, Double>();
		priorities.put(ActivityType.home.name(), Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "priority_home")));
		priorities.put(ActivityType.visit.name(), Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "priority_visit")));
		priorities.put(ActivityType.culture.name(), Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "priority_culture")));
		priorities.put(ActivityType.gastro.name(), Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "priority_gastro")));
		
		double beta_join = Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "beta_join"));
		double delta_type = Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "delta_type"));
		double beta_act = ((PlanCalcScoreConfigGroup) config.getModule("planCalcScore")).getPerforming_utils_hr() / 3600.0;
		double beta_leg = ((PlanCalcScoreConfigGroup) config.getModule("planCalcScore")).getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0;
	
		if(nonCooperativeMode && beta_join != 0) {
			logger.warn("Simulation runs in non-cooperative mode with beta_join set!");
		}
		EvaluatorComposite evaluator = new EvaluatorComposite();
		evaluator.addComponent(new LegEvaluator(beta_leg));
		evaluator.addComponent(new ActivityEvaluator2(beta_act, personDesires, priorities));
//		evaluator.addComponent(new JointActivityEvaluator(beta_join, physical.getVisitorTracker(), graph));
		double fVisit = Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "alterProba_visit"));
		double fCulture = Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "alterProba_culture"));
		double fGastro = Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "alterProba_gastro"));
//		double beta_coordinate = Double.parseDouble(config.getParam("socialnets", "beta_coordinate"));
		evaluator.addComponent(new JointActivityEvaluator2(beta_join, physical.getVisitorTracker(), graph, fVisit, fCulture, fGastro));
//		evaluator.addComponent(new JointActivityEvaluator(beta_join, physical.getVisitorTracker(), graph));
		evaluator.addComponent(new ActivityTypeEvaluator(delta_type, personDesires));
		
		EvalEngine eval = new EvalEngine(evaluator);
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
		TrajectoryAnalyzerTask task = initAnalyzerTask(physical, eval, mental, sampleInterval);
		simEngine.setAnalyzerTask(task, output);
		
		ActivityFacilitySelector.facilities = facilities;
		
		Profiler.disable();
		simEngine.run(scoreTask, iterations);
	}

	private static void loadData(Config config) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		
		logger.info("Loading network data...");
		LoggerUtils.setVerbose(false);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		network = (NetworkImpl) scenario.getNetwork();
		LoggerUtils.setVerbose(true);
		logger.info(String.format("%1$s nodes, %2$s links.", network.getNodes().size(), network.getLinks().size()));
		
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
	
	private static void loadPlans(String file) {
		Scenario scenario = new ScenarioBuilder(config).setNetwork(network).build() ;
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile(file);
		Population pop = scenario.getPopulation();
		for(SocialVertex v : graph.getVertices()) {
			Person person = v.getPerson().getPerson();
			Plan plan  = pop.getPersons().get(person.getId()).getSelectedPlan();

			person.getPlans().clear();
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			plan.setPerson(person);
		}
	}
	
	private static NetworkLegRouter initRouter(final TravelTime travelTime) {
		TravelDisutility travelCost = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				return travelTime.getLinkTravelTime(link, time, person, vehicle);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException();
			}
		};
		
		TravelDisutility travelMinCost = new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				return travelTime.getLinkTravelTime(link, time, person, vehicle);
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return travelTime.getLinkTravelTime(link, 0, null, null);
			}
		};
		
		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, travelMinCost, MultiThreading.getNumAllowedThreads());
		LeastCostPathCalculator router = factory.createPathCalculator(network, travelCost, travelTime);
		NetworkLegRouter legRouter = new NetworkLegRouter(network, router, new RouteFactoryImpl());
		
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
		Map<SocialVertex, String> types = new HashMap<SocialVertex, String>();
		for(SocialVertex v : graph.getVertices()) {
			ActivityDesires desire = personDesires.get(v.getPerson().getPerson());
			types.put(v, desire.getActivityType());
		}
		FixedActivityTypeSelector actTypeSelector = new FixedActivityTypeSelector(types);
//		ChoiceSet<String> choiceSet = new ChoiceSet<String>(random);
//		choiceSet.addOption(ActivityType.visit.name());
//		choiceSet.addOption(ActivityType.gastro.name());
//		choiceSet.addOption(ActivityType.culture.name());
//		ActivityTypeSelector actTypeSelector = new ActivityTypeSelector(choiceSet);
		choiceSelector.addComponent(actTypeSelector);
		
		/*
		 * initialize group selector
		 */
		ActivityGroupSelector groupSelector = new ActivityGroupSelector();
		ActivityGroupGenerator generator = null;
		if (nonCooperativeMode) {
			generator = new OnlyEgo();
			groupSelector.addGenerator(ActivityType.visit.name(), generator);
			groupSelector.addGenerator(ActivityType.gastro.name(), generator);
			groupSelector.addGenerator(ActivityType.culture.name(), generator);
		} else {
			String type = config.getParam("socialnets", "groupGenerator");
			if (type.equals("randomAlter")) {
				generator = new RandomAlter(random);
				groupSelector.addGenerator(ActivityType.visit.name(), generator);
				groupSelector.addGenerator(ActivityType.gastro.name(), generator);
				groupSelector.addGenerator(ActivityType.culture.name(), generator);
			} else if (type.equals("randomAlters")) {
				double p = Double.parseDouble(config.getParam("socialnets", "alterProba_visit"));
//				groupSelector.addGenerator(ActivityType.visit.name(), new RandomAlters2(p, random));
				groupSelector.addGenerator(ActivityType.visit.name(), new RandomAlters3(random));
//				groupSelector.addGenerator(ActivityType.visit.name(), new RandomAltersFixed((int) p, random));

				p = Double.parseDouble(config.getParam("socialnets", "alterProba_culture"));
//				groupSelector.addGenerator(ActivityType.culture.name(), new RandomAltersFixed((int) p, random));
//				groupSelector.addGenerator(ActivityType.culture.name(), new RandomAlters2(p, random));
				groupSelector.addGenerator(ActivityType.culture.name(), new RandomAlters3(random));

				p = Double.parseDouble(config.getParam("socialnets", "alterProba_gastro"));
//				groupSelector.addGenerator(ActivityType.gastro.name(), new RandomAltersFixed((int) p, random));
//				groupSelector.addGenerator(ActivityType.gastro.name(), new RandomAlters2(p, random));
				groupSelector.addGenerator(ActivityType.gastro.name(), new RandomAlters3(random));
			} else {
				throw new IllegalArgumentException(String.format("Activity group generator \"%1$s\" unknown.", type));
			}
		}
		choiceSelector.addComponent(groupSelector);
		/*
		 * initialize facility selector
		 */
		ActivityFacilitySelector facilitySelector = new ActivityFacilitySelector();

		if(nonCooperativeMode) {
			facilitySelector.addGenerator(ActivityType.visit.name(), new AltersHome(random));
		} else {
			facilitySelector.addGenerator(ActivityType.visit.name(), new EgosHome(random));
		}

//		FacilityChoiceSetGenerator generator2 = new FacilityChoiceSetGenerator(-1.2, 5, random, CartesianDistanceCalculator.getInstance());
//		try {
//			generator2.write(generator2.generate(graph, facilities, "gastro"), "/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.gastro.145412.12.txt");
//			generator2.write(generator2.generate(graph, facilities, "culture"), "/Users/jillenberger/Work/socialnets/locationChoice/data/choiceset.culture.145412.12.txt");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		System.exit(0);
		facilitySelector.addGenerator(ActivityType.gastro.name(), new EgosFacilities(FacilityChoiceSetGenerator.read(config.getParam(SOCNET_MODULE_NAME, "choiceset_gastro"), graph), random));
		facilitySelector.addGenerator(ActivityType.culture.name(), new EgosFacilities(FacilityChoiceSetGenerator.read(config.getParam(SOCNET_MODULE_NAME, "choiceset_culture"), graph), random));

		choiceSelector.addComponent(facilitySelector);
		/*
		 * initialize arrival time selector
		 */
		ActTypeTimeSelector arrTimeSelector = new ActTypeTimeSelector();
		choiceSelector.addComponent(arrTimeSelector);
		/*
		 * initialize duration selector
		 */
		ActTypeTimeSelector durTimeSelector = new ActTypeTimeSelector();
		choiceSelector.addComponent(durTimeSelector);
		
		//visit
		Map<SocialVertex, Double> arrivals = new HashMap<SocialVertex, Double>();
		Map<SocialVertex, Double> durations = new HashMap<SocialVertex, Double>();
		for(Entry<SocialVertex, ActivityDesires> entry : vertexDesires.entrySet()) {
			ActivityDesires desire = entry.getValue();
			arrivals.put(entry.getKey(), desire.getActivityStartTime(ActivityType.visit.name()));
			durations.put(entry.getKey(), desire.getActivityDuration(ActivityType.visit.name()));
		}
		
		arrTimeSelector.addSelector(ActivityType.visit.name(), new ArrivalTimeSelector(arrivals, random));
		durTimeSelector.addSelector(ActivityType.visit.name(), new DurationSelector(durations, random));
		
		//culture
		arrivals = new HashMap<SocialVertex, Double>();
		durations = new HashMap<SocialVertex, Double>();
		for(Entry<SocialVertex, ActivityDesires> entry : vertexDesires.entrySet()) {
			ActivityDesires desire = entry.getValue();
			arrivals.put(entry.getKey(), desire.getActivityStartTime(ActivityType.culture.name()));
			durations.put(entry.getKey(), desire.getActivityDuration(ActivityType.culture.name()));
		}
		
		arrTimeSelector.addSelector(ActivityType.culture.name(), new ArrivalTimeSelector(arrivals, random));
		durTimeSelector.addSelector(ActivityType.culture.name(), new DurationSelector(durations, random));
		
		//gastro
		arrivals = new HashMap<SocialVertex, Double>();
		durations = new HashMap<SocialVertex, Double>();
		for(Entry<SocialVertex, ActivityDesires> entry : vertexDesires.entrySet()) {
			ActivityDesires desire = entry.getValue();
			arrivals.put(entry.getKey(), desire.getActivityStartTime(ActivityType.gastro.name()));
			durations.put(entry.getKey(), desire.getActivityDuration(ActivityType.gastro.name()));
		}
		
		arrTimeSelector.addSelector(ActivityType.gastro.name(), new ArrivalTimeSelector(arrivals, random));
		durTimeSelector.addSelector(ActivityType.gastro.name(), new DurationSelector(durations, random));
		
		return choiceSelector;
	}
	
	private static void loadDesires(String file, Random random) {
		boolean exists = false;
		if(file != null) 
			exists = new File(file).exists();
		
		if(exists) {
			logger.info("Loading desires from file...");
			vertexDesires = ActivityDesires.read(graph, file);
			personDesires = new HashMap<Person, ActivityDesires>();
			for(Entry<SocialVertex, ActivityDesires> entry : vertexDesires.entrySet()) {
				personDesires.put(entry.getKey().getPerson().getPerson(), entry.getValue());
			}
		} else {
			personDesires = new HashMap<Person, ActivityDesires>();
			vertexDesires = new HashMap<SocialVertex, ActivityDesires>();
			/*
			 * activity types
			 */
			ChoiceSet<String> actTypeChoiceSet = new ChoiceSet<String>(random);
			actTypeChoiceSet.addOption(ActivityType.visit.name(), Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "actshare_visit")));
			actTypeChoiceSet.addOption(ActivityType.gastro.name(), Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "actshare_gastro")));
			actTypeChoiceSet.addOption(ActivityType.culture.name(), Double.parseDouble(config.getParam(SOCNET_MODULE_NAME, "actshare_culture")));

			for(SocialVertex v : graph.getVertices()) {
				ActivityDesires desire = personDesires.get(v.getPerson().getPerson());
				if(desire == null) {
					desire = new ActivityDesires();
					personDesires.put(v.getPerson().getPerson(), desire);
					vertexDesires.put(v, desire);
				}
				String type = actTypeChoiceSet.randomWeightedChoice();
				desire.setActivityType(type);
			}
			/*
			 * arrival times and durations
			 */
			//visit
			logger.info("Drawing arrival times and durations for visit...");
			AdditiveDistribution arrTimePDF = new AdditiveDistribution();
			arrTimePDF.addComponent(new GaussDistribution(2645, 41879, 228));
			arrTimePDF.addComponent(new GaussDistribution(9629, 56152, 1148));
			Map<SocialVertex, Double> arrivals = initArrivalTimes(arrTimePDF, 86400, random);
			
			LinearDistribution visitArrDur = new LinearDistribution(-0.2, 22091);
//			Map<SocialVertex, Double> durations = initDurations(arrivals, visitArrDur, 0.8864, 1061, random);
			Map<SocialVertex, Double> durations = initDurations(arrivals, visitArrDur, 6.392550645818048E7, 1061, random);
//			Map<SocialVertex, Double> durations = initDurations(arrivals, visitArrDur, 386473804, 1061, random);
			
			addDesire(durations, arrivals, ActivityType.visit.name());
			//culture
			logger.info("Drawing arrival times and durations for culture...");
			arrTimePDF = new AdditiveDistribution();
			arrTimePDF.addComponent(new GaussDistribution(3246, 36293, 429));
			arrTimePDF.addComponent(new GaussDistribution(7281, 54261, 1046));
			arrivals = initArrivalTimes(arrTimePDF, 86400, random);
			
			PowerLawDistribution cultureArrDur = new PowerLawDistribution(-1.28, 9.528E09);
//			durations = initDurations(arrivals, cultureArrDur, 0.9609, 1093, random);
			durations = initDurations(arrivals, cultureArrDur, 5.367723221521734E7, 1093, random);
//			durations = initDurations(arrivals, cultureArrDur, 169505132, 1093, random);
			
			addDesire(durations, arrivals, ActivityType.culture.name());
			//gastro
			logger.info("Drawing arrival times and durations for gastro...");
			arrTimePDF = new AdditiveDistribution();
			arrTimePDF.addComponent(new GaussDistribution(3071, 43340, 630));
			arrTimePDF.addComponent(new GaussDistribution(6236, 68048, 656));
			arrTimePDF.addComponent(new GaussDistribution(3938, 54224, 499));
			arrivals = initArrivalTimes(arrTimePDF, 86400, random);
			
			AdditiveDistribution arrDurPDF = new AdditiveDistribution();
			arrDurPDF.addComponent(new GaussDistribution(8185, 42524, 168916676));
			arrDurPDF.addComponent(new GaussDistribution(9321, 69593, 169103450));
//			durations = initDurations(arrivals, arrDurPDF, 0.6883, 522, random);
			durations = initDurations(arrivals, arrDurPDF, 2.4991600411477614E7, 522, random);
//			durations = initDurations(arrivals, arrDurPDF, 23294057, 522, random);

			addDesire(durations, arrivals, ActivityType.gastro.name());
			
			if(file != null)
				ActivityDesires.write(personDesires, file);
		}
	}
	
	private static void addDesire(Map<SocialVertex, Double> durations, Map<SocialVertex, Double> arrivals, String type) {
		for(java.util.Map.Entry<SocialVertex, Double> entry : durations.entrySet()) {
			ActivityDesires desires2 = personDesires.get(entry.getKey().getPerson().getPerson());
			if(desires2 == null) {
				desires2 = new ActivityDesires();
				personDesires.put(entry.getKey().getPerson().getPerson(), desires2);
			}
			desires2.putActivityDuration(type, entry.getValue());
			desires2.putActivityStartTime(type, arrivals.get(entry.getKey()));
		}
	}
	
	private static Map<SocialVertex, Double> initArrivalTimes(UnivariateRealFunction pdf, int max, Random random) {
		TimeSampler sampler = new TimeSampler(pdf, max, random);
		Map<SocialVertex, Double> map = new HashMap<SocialVertex, Double>(graph.getVertices().size());

		for(SocialVertex v : graph.getVertices()) {
			double sample =sampler.nextSample();
			map.put(v, sample);
		}
		
		return map;
	}
	
	private static Map<SocialVertex, Double> initDurations(Map<SocialVertex, Double> arrivals, UnivariateRealFunction arrDur, double var, double lin, Random random) {
		Map<SocialVertex, Double> durations = new HashMap<SocialVertex, Double>();
		for(SocialVertex v : graph.getVertices()) {
			double t_arr = arrivals.get(v);
			double dur_mean;
			try {
				dur_mean = arrDur.value(t_arr);
				if(dur_mean > 0) {
					double sigma_2 = Math.log(1 + (var/Math.pow(dur_mean, 2)));
					double mu = Math.log(dur_mean) - (sigma_2/2.0);
					
					TimeSampler sampler = new TimeSampler(new LogNormalDistribution(Math.sqrt(sigma_2), mu, lin), 86400, random);
					double dur = sampler.nextSample();
					durations.put(v, dur);
					
				} else {
					throw new RuntimeException("Zeor duration.");
				}
				
			} catch (FunctionEvaluationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return durations;
	}
	
	private static TrajectoryAnalyzerTask initAnalyzerTask(PhysicalEngine physical, EvalEngine eval, MentalEngine mentalEngine, int logInterval) {
		TrajectoryAnalyzerTaskComposite composite = new TrajectoryAnalyzerTaskComposite();
		composite.addTask(new PlansWriterTask(network));
		composite.addTask(new ArrivalTimeTask());
		composite.addTask(new ActivityDurationTask());
		composite.addTask(new TripGeoDistanceTask(facilities, CartesianDistanceCalculator.getInstance()));
		composite.addTask(new TripDurationTask());
		composite.addTask(new JointActivityTask(graph, physical.getVisitorTracker()));
		scoreTask = new ScoreTask();
		composite.addTask(scoreTask);
		composite.addTask(new ActTypeShareTask());
		composite.addTask(new InfiniteScoresTask());
		composite.addTask(new TransitionProbaAnalyzer(mentalEngine, logInterval));
		composite.addTask(new ActivityLoadTask());
		composite.addTask(new DurationArrivalTimeTask());
		composite.addTask(new LegLoadTask());
		composite.addTask(new DesiredTimeDiffTask(personDesires));
//		composite.addTask(new VisitorsAccessibilityTask(physical.getVisitorTracker(), graph));
		composite.addTask(new DistanceVisitorsTask(physical.getVisitorTracker(), graph, facilities));
		composite.addTask(new CoordinationComplexityTask(physical.getVisitorTracker(), personDesires, graph));
		composite.addTask(new DepartureLoadTask());
		composite.addTask(new TripPurposeShareTask());
		composite.addTask(new DistanceArrivalTimeTask(new TripDistanceMean(null, facilities, CartesianDistanceCalculator.getInstance())));
		composite.addTask(new TripDurationArrivalTime());
		return composite;
	}
	
	private static class AdaptorFactory implements Choice2ModAdaptorFactory {

		private final PhysicalEngine physical;
		
		public AdaptorFactory(PhysicalEngine physical) {
			this.physical = physical;
		}
		
		@Override
		public Choice2ModAdaptor create() {
			Choice2ModAdaptorComposite adaptor = new Choice2ModAdaptorComposite();
			adaptor.addComponent(new ActivityTypeModAdaptor());
			adaptor.addComponent(new ActivityFacilityModAdaptor(facilities, initRouter(physical.getTravelTime())));
			adaptor.addComponent(new ArrivalTimeModAdaptor());
			adaptor.addComponent(new ActivityDurationModAdaptor());
			return adaptor;
		}
		
	}
	
	private static enum ActivityType {
		
		home, visit, culture, gastro
		
	}
}
