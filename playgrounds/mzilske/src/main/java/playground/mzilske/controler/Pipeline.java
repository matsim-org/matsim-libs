///* *********************************************************************** *
//// * project: org.matsim.*
// * Controler.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.mzilske.controler;
//
//import java.io.File;
//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.LinkedList;
//import java.util.List;
//
//import org.apache.log4j.Logger;
//import org.matsim.analysis.CalcLegTimes;
//import org.matsim.analysis.CalcLinkStats;
//import org.matsim.analysis.IterationStopWatch;
//import org.matsim.analysis.VolumesAnalyzer;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.ScenarioImpl;
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.api.core.v01.population.PopulationWriter;
//import org.matsim.core.api.experimental.network.NetworkWriter;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigWriter;
//import org.matsim.core.config.groups.StrategyConfigGroup;
//import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
//import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
//import org.matsim.core.controler.ControlerIO;
//import org.matsim.core.controler.corelisteners.PlansScoring;
//import org.matsim.core.events.EventsManagerImpl;
//import org.matsim.core.events.algorithms.EventWriter;
//import org.matsim.core.events.algorithms.EventWriterTXT;
//import org.matsim.core.events.algorithms.EventWriterXML;
//import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.mobsim.framework.MobsimFactory;
//import org.matsim.core.mobsim.framework.Simulation;
//import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
//import org.matsim.core.network.NetworkImpl;
//import org.matsim.core.replanning.PlanStrategyImpl;
//import org.matsim.core.replanning.StrategyManager;
//import org.matsim.core.replanning.StrategyManagerConfigLoader;
//import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
//import org.matsim.core.replanning.modules.ChangeLegMode;
//import org.matsim.core.replanning.modules.SubtourModeChoice;
//import org.matsim.core.replanning.modules.ReRouteDijkstra;
//import org.matsim.core.replanning.modules.ReRouteLandmarks;
//import org.matsim.core.replanning.modules.TimeAllocationMutator;
//import org.matsim.core.replanning.selectors.BestPlanSelector;
//import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
//import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
//import org.matsim.core.replanning.selectors.KeepSelected;
//import org.matsim.core.replanning.selectors.RandomPlanSelector;
//import org.matsim.core.router.PlansCalcRoute;
//import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
//import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
//import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
//import org.matsim.core.router.util.AStarLandmarksFactory;
//import org.matsim.core.router.util.DijkstraFactory;
//import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
//import org.matsim.core.router.util.LeastCostPathCalculatorInvertedNetProxyFactory;
//import org.matsim.core.router.util.PersonalizableTravelCost;
//import org.matsim.core.router.util.PersonalizableTravelTime;
//import org.matsim.core.scoring.EventsToScore;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
//import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
//import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
//import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
//import org.matsim.core.utils.misc.Time;
//import org.matsim.population.algorithms.AbstractPersonAlgorithm;
//import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
//import org.matsim.population.algorithms.PermissibleModesCalculator;
//import org.matsim.population.algorithms.PersonPrepareForSim;
//import org.matsim.population.algorithms.PlanAlgorithm;
//import org.matsim.vehicles.Vehicle;
//
//import playground.anhorni.LinkStatsAnalyser;
//import playground.mzilske.deteval.VehicleWatchingEventHandler;
//
//
//public final class Pipeline {
//
//	public static final String DIRECTORY_ITERS = "ITERS";
//	public static final String FILENAME_EVENTS_TXT = "events.txt.gz";
//	public static final String FILENAME_EVENTS_XML = "events.xml.gz";
//	public static final String FILENAME_LINKSTATS = "linkstats.txt";
//	public static final String FILENAME_SCORESTATS = "scorestats.txt";
//	public static final String FILENAME_TRAVELDISTANCESTATS = "traveldistancestats.txt";
//	public static final String FILENAME_POPULATION = "output_plans.xml.gz";
//	public static final String FILENAME_NETWORK = "output_network.xml.gz";
//	public static final String FILENAME_HOUSEHOLDS = "output_households.xml.gz";
//	public static final String FILENAME_LANES = "output_lanes.xml.gz";
//	public static final String FILENAME_SIGNALSYSTEMS = "output_signalsystems.xml.gz";
//	public static final String FILENAME_SIGNALSYSTEMS_CONFIG = "output_signalsystem_configuration.xml.gz";
//	public static final String FILENAME_CONFIG = "output_config.xml.gz";
//
//	private enum ControlerState {
//		Init, Running, Shutdown, Finished
//	}
//
//	private ControlerState state = ControlerState.Init;
//
//	private String outputPath = null;
//
//	private boolean overwriteFiles = true;
//
//	private Integer iteration = null;
//
//	/** The Config instance the Controler uses. */
//	private final Config config;
//
//	private EventsManagerImpl events = null;
//
//	private final IterationStopWatch stopwatch = new IterationStopWatch();
//
//	final private Scenario scenarioData;
//
//	/**
//	 * Attribute for the routing factory
//	 */
//	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
//
//	private Thread shutdownHook = new Thread() {
//		@Override
//		public void run() {
//			ControlerState oldState = state;
//			state = ControlerState.Shutdown;
//			if (oldState == ControlerState.Running) {
//				log.warn("S H U T D O W N   ---   received unexpected shutdown request.");
//				coreShutdown();
//				// dump plans
//				new PopulationWriter(scenarioData.getPopulation(), scenarioData.getNetwork()).write(controlerIO.getOutputFilename(FILENAME_POPULATION));
//				// dump network
//				new NetworkWriter(scenarioData.getNetwork()).write(controlerIO.getOutputFilename(FILENAME_NETWORK));
//				// dump config
//				new ConfigWriter(config).write(controlerIO.getOutputFilename(FILENAME_CONFIG));
//				log.info("S H U T D O W N   ---   unexpected shutdown request completed.");
//				try {
//					Runtime.getRuntime().removeShutdownHook(shutdownHook);
//				} catch (IllegalStateException e) {
//					log.info("Cannot remove shutdown hook. " + e.getMessage());
//				}
//				shutdownHook = null; // important for test cases to free the memory
//			}
//		}
//	};
//
//	private ControlerIO controlerIO;
//
//	private MobsimFactory mobsimFactory;
//
//	private EventsToScore planScorer;
//
//	private StrategyManager strategyManager;
//
//	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);
//	
//	private static int externalCounter = 0;
//
//	public Pipeline(final Scenario scenario) {
//		this.scenarioData = scenario;
//		this.config = scenario.getConfig();
//		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
//	}
//
//
//	public void prepare(Config config) {	
//		buildTasks();
//		connectTasks();	
//	}
//
//	private void buildTasks() {
//		
//		StrategyManager manager = new StrategyManager();
//		manager.setMaxPlansPerAgent(this.config.strategy().getMaxAgentPlanMemorySize());
//		for (StrategyConfigGroup.StrategySettings settings : this.config.strategy().getStrategySettings()) {
//			double rate = settings.getProbability();
//			if (rate == 0.0) {
//				continue;
//			}
//			String classname = settings.getModuleName();
//		
//			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
//				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
//			}
//		
//			PlanStrategyImpl strategy = loadStrategy(classname, settings);
//		
//			if (strategy == null) {
//				Gbl.errorMsg("Could not initialize strategy named " + classname);
//			}
//		
//			manager.addStrategy(strategy, rate);
//		
//			// now check if this modules should be disabled after some iterations
//			if (settings.getDisableAfter() >= 0) {
//				int maxIter = settings.getDisableAfter();
//				if (maxIter >= this.config.controler().getFirstIteration()) {
//					manager.addChangeRequest(maxIter + 1, strategy, 0.0);
//				} else {
//					/* The controler starts at a later iteration than this change request is scheduled for.
//					 * make the change right now.					 */
//					manager.changeWeightOfStrategy(strategy, 0.0);
//				}
//			}
//		}
//		this.strategyManager = manager;
//		TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
//		TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
//		final String PARALLEL_EVENT_HANDLING = "parallelEventHandling";
//		final String NUMBER_OF_THREADS = "numberOfThreads";
//		final String ESTIMATED_NUMBER_OF_EVENTS = "estimatedNumberOfEvents";
//		String numberOfThreads = this.config.findParam(PARALLEL_EVENT_HANDLING, NUMBER_OF_THREADS);
//		String estimatedNumberOfEvents = this.config.findParam(PARALLEL_EVENT_HANDLING, ESTIMATED_NUMBER_OF_EVENTS);
//
//		if (numberOfThreads != null) {
//			int numOfThreads = Integer.parseInt(numberOfThreads);
//			// the user wants to user parallel events handling
//			if (estimatedNumberOfEvents != null) {
//				int estNumberOfEvents = Integer.parseInt(estimatedNumberOfEvents);
//				this.events = new ParallelEventsManagerImpl(numOfThreads, estNumberOfEvents);
//			} else {
//				this.events = new ParallelEventsManagerImpl(numOfThreads);
//			}
//		} else {
//			this.events = new EventsManagerImpl();
//		}
//		vehicleWatcher = new VehicleWatchingEventHandler();
//		TravelTimeCalculator travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(this.scenarioData.getNetwork(), config.travelTimeCalculator());
//		if (config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.Dijkstra)) {
//			this.leastCostPathCalculatorFactory = new DijkstraFactory();
//		} else if (config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.AStarLandmarks)) {
//			this.leastCostPathCalculatorFactory = new AStarLandmarksFactory(this.scenarioData.getNetwork(), new FreespeedTravelTimeCost(this.config.charyparNagelScoring()));
//		} else {
//			throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
//		}
//
//		if (this.config.controler().isLinkToLinkRoutingEnabled()) {
//			this.leastCostPathCalculatorFactory = new LeastCostPathCalculatorInvertedNetProxyFactory(
//					this.leastCostPathCalculatorFactory);
//		}
//		CalcLinkStats linkStats = new CalcLinkStats(this.scenarioData.getNetwork());
//		VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 24 * 3600 - 1, this.scenarioData.getNetwork());
//		CalcLegTimes legTimes = new CalcLegTimes(this.scenarioData.getPopulation());
//		setUpOutputDir();
//		
//		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(this.config.charyparNagelScoring());
//		this.planScorer = new EventsToScore(this.scenarioData.getPopulation(), scoringFunctionFactory, this.config.charyparNagelScoring().getLearningRate());
//	}
//
//
//	private void connectTasks() {
//		events.addHandler(vehicleWatcher);
//		events.addHandler(this.travelTimeCalculator);
//		events.addHandler(this.legTimes);
//		events.addHandler(this.planScorer);
//	}
//
//
//	public void run() {
//		if (this.state == ControlerState.Init) {
//			ParallelPersonAlgorithmRunner.run(this.scenarioData.getPopulation(), this.config.global().getNumberOfThreads(),
//					new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
//				public AbstractPersonAlgorithm getPersonAlgorithm() {
//					return new PersonPrepareForSim(createRoutingAlgorithm(), (NetworkImpl) scenarioData.getNetwork());
//				}
//			});
//			int firstIteration = this.config.controler().getFirstIteration();
//			int lastIteration = this.config.controler().getLastIteration();
//			this.state = ControlerState.Running;
//			String divider = "###################################################";
//			String marker = "### ";
//
//			for (this.iteration = firstIteration; (this.iteration <= lastIteration) && (this.state == ControlerState.Running); this.iteration++) {
//				log.info(divider);
//				log.info(marker + "ITERATION " + this.iteration + " BEGINS");
//				this.stopwatch.setCurrentIteration(this.iteration);
//				this.stopwatch.beginOperation("iteration");
//				makeIterationPath(this.iteration);
//				resetRandomNumbers();
//
//				this.planScorer.reset(iteration);
//				if (this.iteration > firstIteration) {
//					this.stopwatch.beginOperation("replanning");
//					replanningReplanning(iteration);
//					this.stopwatch.endOperation("replanning");
//				}
//				coreBeforeMobsim(iteration);
//				dumpPlansBeforeMobsim(this.iteration);
//				this.stopwatch.beginOperation("mobsim");
//				resetRandomNumbers();
//				runMobSim();
//				this.stopwatch.endOperation("mobsim");
//				log.info(marker + "ITERATION " + this.iteration + " fires after mobsim event");
//				coreAfterMobsim(iteration);
//				log.info(marker + "ITERATION " + this.iteration + " fires scoring event");
//				this.planScorer.finish();
//				log.info(marker + "ITERATION " + this.iteration + " fires iteration end event");
//				this.stopwatch.endOperation("iteration");
//				this.stopwatch.write(this.controlerIO.getOutputFilename("stopwatch.txt"));
//				log.info(marker + "ITERATION " + this.iteration + " ENDS");
//				log.info(divider);
//			}
//			this.iteration = null;
//			ControlerState oldState = this.state;
//			this.state = ControlerState.Shutdown;
//			if (oldState == ControlerState.Running) {
//				{
//					log.info("S H U T D O W N   ---   start regular shutdown.");
//				}
//				coreShutdown();
//				// dump plans
//				new PopulationWriter(this.scenarioData.getPopulation(), this.scenarioData.getNetwork()).write(this.controlerIO.getOutputFilename(FILENAME_POPULATION));
//				// dump network
//				new NetworkWriter(this.scenarioData.getNetwork()).write(this.controlerIO.getOutputFilename(FILENAME_NETWORK));
//				// dump config
//				new ConfigWriter(this.config).write(this.controlerIO.getOutputFilename(FILENAME_CONFIG));
//
//
//				{
//					log.info("S H U T D O W N   ---   regular shutdown completed.");
//				}
//				try {
//					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
//				} catch (IllegalStateException e) {
//					log.info("Cannot remove shutdown hook. " + e.getMessage());
//				}
//				this.shutdownHook = null; // important for test cases to free the memory
//			}
//		} else {
//			throw new RuntimeException("Controler in wrong state to call 'run()'. Expected state: <Init> but was <" + this.state + ">");
//		}
//	}
//
//	private final List<EventWriter> eventWriters = new LinkedList<EventWriter>();
//
//	private VehicleWatchingEventHandler vehicleWatcher;
//
//	private final void setUpOutputDir() {
//		outputPath = this.config.controler().getOutputDirectory();
//		if (outputPath.endsWith("/")) {
//			outputPath = outputPath.substring(0, outputPath.length() - 1);
//		}
//		if (this.config.controler().getRunId() != null) {
//			this.controlerIO =  new ControlerIO(outputPath, this.scenarioData.createId(this.config.controler().getRunId()));
//		}
//		else {
//			this.controlerIO =  new ControlerIO(outputPath);
//		}
//
//		// make the tmp directory
//		File outputDir = new File(outputPath);
//		if (outputDir.exists()) {
//			if (outputDir.isFile()) {
//				throw new RuntimeException("Cannot create output directory. " + outputPath
//						+ " is a file and cannot be replaced by a directory.");
//			}
//			if (outputDir.list().length > 0) {
//				if (this.overwriteFiles) {
//					log.warn("###########################################################");
//					log.warn("### THE CONTROLER WILL OVERWRITE FILES IN:");
//					log.warn("### " + outputPath);
//					log.warn("###########################################################");
//				} else {
//					// the directory is not empty, we do not overwrite any
//					// files!
//					throw new RuntimeException(
//							"The output directory "
//							+ outputPath
//							+ " exists already but has files in it! Please delete its content or the directory and start again. We will not delete or overwrite any existing files.");
//				}
//			}
//		} else {
//			if (!outputDir.mkdirs()) {
//				throw new RuntimeException("The output directory path " + outputPath
//						+ " could not be created. Check pathname and permissions!");
//			}
//		}
//
//		File tmpDir = new File(this.controlerIO.getTempPath());
//		if (!tmpDir.mkdir() && !tmpDir.exists()) {
//			throw new RuntimeException("The tmp directory " + this.controlerIO.getTempPath() + " could not be created.");
//		}
//		File itersDir = new File(outputPath + "/" + DIRECTORY_ITERS);
//		if (!itersDir.mkdir() && !itersDir.exists()) {
//			throw new RuntimeException("The iterations directory " + (outputPath + "/" + DIRECTORY_ITERS)
//					+ " could not be created.");
//		}
//	}
//
//
//	/**
//	 * Creates the path where all iteration-related data should be stored.
//	 *
//	 * @param iteration
//	 */
//	private void makeIterationPath(final int iteration) {
//		File dir = new File(this.controlerIO.getIterationPath(iteration));
//		if (!dir.mkdir()) {
//			if (this.overwriteFiles && dir.exists()) {
//				log.info("Iteration directory " + this.controlerIO.getIterationPath(iteration) + " exists already.");
//			} else {
//				log.warn("Could not create iteration directory " + this.controlerIO.getIterationPath(iteration) + ".");
//			}
//		}
//	}
//
//	private void resetRandomNumbers() {
//		MatsimRandom.reset(this.config.global().getRandomSeed() + this.iteration);
//		MatsimRandom.getRandom().nextDouble(); // draw one because of strange
//		// "not-randomness" is the first
//		// draw...
//		// Fixme [kn] this should really be ten thousand draws instead of just
//		// one
//	}
//
//	private void runMobSim() {
//		Simulation simulation = mobsimFactory.createMobsim(this.scenarioData, this.events);
//		simulation.run();
//	}
//
//	private PlanAlgorithm createRoutingAlgorithm() {
//		return new PlansCalcRoute(this.config.plansCalcRoute(), this.scenarioData.getNetwork(), this.travelCostCalculatorFactory.createTravelCostCalculator(this.travelTimeCalculator, config.charyparNagelScoring()), this.travelTimeCalculator, this.leastCostPathCalculatorFactory);
//	}
//
//	private void coreBeforeMobsim(int iteration) {
//		events.resetHandlers(iteration);
//		events.resetCounter();
//
//		for (EventsFileFormat format : config.controler().getEventsFileFormats()) {
//			switch (format) {
//			case txt:
//				this.eventWriters.add(new EventWriterTXT(this.controlerIO.getIterationFilename(iteration,FILENAME_EVENTS_TXT)));
//				break;
//			case xml:
//				this.eventWriters.add(new EventWriterXML(this.controlerIO.getIterationFilename(iteration, FILENAME_EVENTS_XML)));
//				break;
//			default:
//				log.warn("Unknown events file format specified: " + format.toString() + ".");
//			}
//		}
//		for (EventWriter writer : this.eventWriters) {
//			events.addHandler(writer);
//		}
//
//		if (iteration % 10 == 6) {
//			volumes.reset(iteration);
//			events.addHandler(volumes);
//		}
//
//		// init for event processing of new iteration
//		events.initProcessing();
//	}
//
//	private void coreAfterMobsim(int iteration) {
//
//		// prepare for finishing iteration
//		events.finishProcessing();
//
//		for (EventWriter writer : this.eventWriters) {
//			writer.closeFile();
//			events.removeHandler(writer);
//		}
//		this.eventWriters.clear();
//
//		if (((iteration % 10 == 0) && (iteration > this.config.controler().getFirstIteration())) || (iteration % 10 >= 6)) {
//			linkStats.addData(volumes, travelTimeCalculator);
//		}
//
//		if ((iteration % 10 == 0) && (iteration > this.config.controler().getFirstIteration())) {
//			events.removeHandler(volumes);
//			linkStats.writeFile(this.controlerIO.getIterationFilename(iteration, FILENAME_LINKSTATS));
//		}
//
//		if (legTimes != null) {
//			legTimes.writeStats(this.controlerIO.getIterationFilename(iteration, "tripdurations.txt"));
//			// - print averages in log
//			log.info("[" + iteration + "] average trip duration is: " + (int) legTimes.getAverageTripDuration()
//					+ " seconds = " + Time.writeTime(legTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));
//		}
//	}
//
//	private void coreShutdown() {
//		for (EventWriter writer : eventWriters) {
//			writer.closeFile();
//		}
//		vehicleWatcher.dump();
//
//	}
//
//	private void dumpPlansBeforeMobsim(int iteration) {
//		if ((iteration % 10 == 0) || (iteration == (this.config.controler().getFirstIteration() + 1))) {
//			stopwatch.beginOperation("dump all plans");
//			log.info("dumping plans...");
//			new PopulationWriter(this.scenarioData.getPopulation(), this.scenarioData.getNetwork())
//			.write(this.controlerIO.getIterationFilename(iteration, "plans.xml.gz"));
//			log.info("finished plans dump.");
//			stopwatch.endOperation("dump all plans");
//		}
//	}
//
//	private PlanStrategyImpl loadStrategy(final String name, final StrategyConfigGroup.StrategySettings settings) {
//		Network network = this.scenarioData.getNetwork();
//		PersonalizableTravelCost travelCostCalc = this.travelCostCalculatorFactory.createTravelCostCalculator(this.travelTimeCalculator, config.charyparNagelScoring());
//		PersonalizableTravelTime travelTimeCalc = this.travelTimeCalculator;
//		Config config = this.config;
//
//		PlanStrategyImpl strategy = null;
//		if (name.equals("KeepLastSelected")) {
//			strategy = new PlanStrategyImpl(new KeepSelected());
//		} else if (name.equals("ReRoute") || name.equals("threaded.ReRoute")) {
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//			strategy.addStrategyModule(new ReRoute(this));
//		} else if (name.equals("ReRoute_Dijkstra")) {
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//			strategy.addStrategyModule(new ReRouteDijkstra(config, network, travelCostCalc, travelTimeCalc));
//		} else if (name.equals("ReRoute_Landmarks")) {
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//			strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
//		} else if (name.equals("TimeAllocationMutator") || name.equals("threaded.TimeAllocationMutator")) {
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//			TimeAllocationMutator tam = new TimeAllocationMutator(config);
//			//			tam.setUseActivityDurations(config.vspExperimental().isUseActivityDurations());
//			// functionality moved into TimeAllocationMutator.  kai, aug'10
//			strategy.addStrategyModule(tam);
//		} else if (name.equals("TimeAllocationMutator7200_ReRouteLandmarks")) {
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//			strategy.addStrategyModule(new TimeAllocationMutator(config, 7200));
//			strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
//		} else if (name.equals("ExternalModule")) {
//			externalCounter++;
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//			String exePath = settings.getExePath();
//			ExternalModule em = new ExternalModule(exePath, "ext" + externalCounter, controlerIO, scenarioData, 1);
//			em.setIterationNumber(iteration);
//			strategy.addStrategyModule(em);
//		} else if (name.equals("BestScore")) {
//			strategy = new PlanStrategyImpl(new BestPlanSelector());
//		} else if (name.equals("SelectExpBeta")) {
//			strategy = new PlanStrategyImpl(new ExpBetaPlanSelector(config.charyparNagelScoring()));
//		} else if (name.equals("ChangeExpBeta")) {
//			strategy = new PlanStrategyImpl(new ExpBetaPlanChanger(config.charyparNagelScoring().getBrainExpBeta()));
//		} else if (name.equals("SelectRandom")) {
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//		} else if (name.equals("ChangeLegMode")) {
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//			strategy.addStrategyModule(new ChangeLegMode(config));
//			strategy.addStrategyModule(new ReRoute(this));
//		} else if (name.equals("SubtourChangeLegMode")) {
//			strategy = new PlanStrategyImpl(new RandomPlanSelector());
//			SubtourModeChoice changeLegMode = new SubtourModeChoice(config);
//			changeLegMode.setPermissibleModesCalculator(new PermissibleModesCalculator() {
//
//				@Override
//				public Collection<String> getPermissibleModes(Plan plan) {
//					Person person = plan.getPerson();
//					Vehicle vehicle = ((ScenarioImpl) scenarioData).getVehicles().getVehicles().get(person.getId());
//					if (vehicle != null) {
//						log.info(person.getId() + " has a car.");
//						return Arrays.asList(TransportMode.car, TransportMode.pt);
//					} else {
//						log.info(person.getId() + " has no car.");
//						return Arrays.asList(TransportMode.pt);
//					}
//				}
//
//			});
//			strategy.addStrategyModule(changeLegMode);
//			strategy.addStrategyModule(new ReRoute(this));
//		} else {
//			//classes loaded by name must not be part of the matsim core
//			if (name.startsWith("org.matsim")) {
//				log.error("Strategies in the org.matsim package must not be loaded by name!");
//			}
//			else {
//				try {
//					Class<? extends PlanStrategyImpl> klas = (Class<? extends PlanStrategyImpl>) Class.forName(name);
//					Class[] args = new Class[1];
//					args[0] = Scenario.class;
//					Constructor<? extends PlanStrategyImpl> c = null;
//					try{
//						c = klas.getConstructor(args);
//						strategy = c.newInstance(scenarioData);
//					} catch(NoSuchMethodException e){
//						log.warn("Cannot find Constructor in PlanStrategy " + name + " with single argument of type Scenario. " +
//								"This is not fatal, trying to find other constructor, however a constructor expecting Scenario as " +
//						"single argument is recommented!" );
//					}
//					if (c == null){
//						args[0] = Pipeline.class;
//						c = klas.getConstructor(args);
//						strategy = c.newInstance(this);
//					}
//					log.info("Loaded PlanStrategy from class " + name);
//				} catch (ClassNotFoundException e) {
//					e.printStackTrace();
//				} catch (InstantiationException e) {
//					e.printStackTrace();
//				} catch (IllegalAccessException e) {
//					e.printStackTrace();
//				} catch (SecurityException e) {
//					e.printStackTrace();
//				} catch (NoSuchMethodException e) {
//					e.printStackTrace();
//				} catch (IllegalArgumentException e) {
//					e.printStackTrace();
//				} catch (InvocationTargetException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		return strategy;
//	}
//
//	private static class ReRoute extends AbstractMultithreadedModule {
//
//		private final Pipeline controler;
//
//		public ReRoute(final Pipeline controler) {
//			super(controler.config.global());
//			this.controler = controler;
//		}
//
//		@Override
//		public PlanAlgorithm getPlanAlgoInstance() {
//			return this.controler.createRoutingAlgorithm();
//		}
//
//	}
//
//
//	private void replanningReplanning(int iteration) {
//		this.strategyManager.run(this.scenarioData.getPopulation(), iteration);
//	}
//
//}
//

