/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.controler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;

import javax.swing.event.EventListenerList;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.controler.corelisteners.LegHistogramListener;
import org.matsim.controler.listener.ControlerListener;
import org.matsim.counts.CountControlerListener;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.ExternalMobsim;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.mobsim.queuesim.Simulation;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAverageScore;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.StrategyManagerConfigLoader;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingScoringFunctionFactory;
import org.matsim.roadpricing.TollTravelCostCalculator;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelCostI;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.stats.PlanStatsManager;
import org.matsim.trafficmonitoring.AbstractTravelTimeCalculator;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;

public class Controler {

	public static final String STATS_MODULE = "stats";
	public static final String STATS_FILE= "statsOutFile";
	public static final String STATS_MANAGER_ACTIVE = "generateStats";

	public static final String FILENAME_EVENTS = "events.txt.gz"; // write compressed files
	public static final String FILENAME_PLANS = "plans.xml";
	public static final String FILENAME_LINKSTATS = "linkstats.att";

//	jhackney
	protected static final String DIRECTORY_ITERS = "ITERS";

	protected final Events events = new Events();
	protected Population population = null;

	protected PlanStatsManager statsManager = null;

// jhackney
	protected Facilities facilities = null;

	private boolean running = false;
	protected StrategyManager strategyManager = null;
	protected NetworkLayer network = null;
	protected AbstractTravelTimeCalculator travelTimeCalculator = null;
	protected TravelCostI travelCostCalculator = null;

//	jhackney
	protected static String outputPath = null;
	private static int iteration = -1;

	protected RoadPricingScheme toll = null;

	protected EventsToScore planScorer = null;
	protected EventWriterTXT eventwriter = null;

	protected CalcPaidToll tollCalc = null;
	protected CalcLinkStats linkStats = null;
	protected CalcLegTimes legTimes = null;
	protected VolumesAnalyzer volumes = null;

// jhackney
	private boolean overwriteFiles = true;

//	jhackney
	protected int minIteration;
//	jhackney
	protected int maxIterations;
	/**
	 * The swing event listener list to manage ControlerListeners efficiently.
	 */
	private final EventListenerList listenerList = new EventListenerList();

	/** Describes whether the output directory is correctly set up and can be used. */
	private boolean outputDirSetup = false;

	/** The Config instance the Controler uses. */
	protected Config config = null;

	protected IterationStopWatch stopwatch = new IterationStopWatch();

	private static final Logger log = Logger.getLogger(Controler.class);

	private static final String logProperties = "log4j.xml";

	private boolean createLegHistogramPNG = true;

	static {
		URL url = Loader.getResource(logProperties);
		if (url != null) {
			PropertyConfigurator.configure(url);
		}
		else {
			Logger root = Logger.getRootLogger();
			root.setLevel(Level.INFO);
			PatternLayout layout = new PatternLayout("%d{ISO8601} %5p %C{1}:%L %m%n");
			ConsoleAppender consoleAppender = new ConsoleAppender(layout, "System.out");
			consoleAppender.setName("A1");
			root.addAppender(consoleAppender);
			consoleAppender.setLayout(layout);
			log.error("");
			log.error("Could not find configuration file " + logProperties + " for Log4j in the classpath.");
			log.error("A default configuration is used, setting log level to INFO with a ConsoleAppender.");
			log.error("");
			log.error("");
		}
	}

	public Controler() {
		super();
		Runtime run = Runtime.getRuntime();
		run.addShutdownHook( new Thread() {
					@Override
			public void run() {
			  shutdown(true);
			}
		});
	}

	public final void run(final String[] args) {
		this.running = true;

		printNote("M A T S I M - C O N T R O L E R", "start");

		this.config = Gbl.getConfig();
		if (this.config == null) {
			if (args.length == 1) {
				this.config = Gbl.createConfig(new String[]{args[0], "config_v1.dtd"});
			}	else {
				this.config = Gbl.createConfig(args);
			}
		} else if ((args != null) && (args.length != 0)) {
			Gbl.errorMsg("config exists already! Cannot create a 2nd global config from args: " + Arrays.toString(args));
		}

		printNote("", "Complete config dump:...");
		ConfigWriter configwriter = new ConfigWriter(this.config, new PrintWriter(System.out));
		configwriter.write();
		printNote("", "Complete config dump: done...");
		if (this.config.counts().getCountsFileName() != null) {
			this.addControlerListener(new CountControlerListener(this.config));
		}
		this.addControlerListener(new LegHistogramListener(this.events, this.createLegHistogramPNG));

		this.minIteration = this.config.controler().getFirstIteration();
		this.maxIterations = this.config.controler().getLastIteration();

		setupOutputDir(); // make sure all required directories exist
		loadData(); // only reading data: network, plans, facilities, ...
		startup(); // init some "global objects", prepare data for simulation, ...

		doIterations();

		shutdown(false);

		printNote("M A T S I M - C O N T R O L E R", "exit");
	}

	protected void runMobSim() {
		SimulationTimer.setTime(0);

		String externalMobsim = this.config.findParam("simulation", "externalExe");
		if (externalMobsim == null) {
			// queue-sim david
			Simulation sim = new QueueSimulation(this.network, this.population, this.events);
			sim.run();
		} else {
			/* remove eventswriter, as the external mobsim has to write the events */
			this.events.removeHandler(this.eventwriter);
			ExternalMobsim sim = new ExternalMobsim(this.population, this.events);
			sim.run();
		}

	}

	private final void doIterations() {

		/* MR dec06
		 * What my goal for Controler.doIterations() is:
		 * for (iteration...) {
		 * 	required_setup_iteration(); // cannot be overwritten by other controlers, e.g. running strategies?
		 *  setup_iteration(); // can be overwritten/extended by other controlers, e.g. for additional analysis-algorithms
		 *  runMobsim(); // can be overwritten, e.g. for using different mobsims
		 *  required_finish_iteration(); // cannot be overwritten by others, e.g. scoring plans
		 *  finish_iteration(); // can be overwritten for additional analysis-output
		 * }
		 *
		 * not sure this makes really sense, e.g. for the toll-cases I'd need a special scoring algorithm.
		 * So I'm not sure the "required_*" routines can really be private, but I dislike to give other
		 * controler-implementations to override the scoring and strategy part too easy...
		 */

		Gbl.startMeasurement();
		for (Controler.iteration = this.minIteration; Controler.iteration <= this.maxIterations; Controler.iteration++) {
			this.stopwatch.setCurrentIteration(Controler.iteration);
			this.stopwatch.beginOperation("iteration");
			printNote("I T E R A T I O N   " + Controler.iteration, "[" + Controler.iteration + "] iteration begins");
			Gbl.printMemoryUsage();
			this.events.resetCounters(Controler.iteration);

			makeIterationPath(Controler.iteration);

			// reset random seed every iteration so we can more easily resume runs
			Gbl.random.setSeed(this.config.global().getRandomSeed() + Controler.iteration);
			Gbl.random.nextDouble(); // draw one because of strange "not-randomness" is the first draw...

			if (this.tollCalc != null) {		// roadPricing only
				this.tollCalc.reset(Controler.iteration);
			}

			//
			// generate new plans for some percentage of population
			//
			if (Controler.iteration > this.minIteration) {
				this.stopwatch.beginOperation("replanning");
				printNote("R E P L A N N I N G   " + Controler.iteration, "[" + Controler.iteration + "] running strategy modules begins");
				this.strategyManager.run(this.population, Controler.iteration);
				printNote("R E P L A N N I N G   " + Controler.iteration, "[" + Controler.iteration + "] running strategy modules ends");
				this.stopwatch.endOperation("replanning");
			}

			printNote("", "[" + Controler.iteration + "] setup iteration");
			Gbl.printMemoryUsage();
			setupIteration(Controler.iteration);

			this.events.printEventHandlers();

			// reset random seed again before mobsim, as we do not know if strategy modules ran and if they used random numbers.
			Gbl.random.setSeed(this.config.global().getRandomSeed() + Controler.iteration);
			Gbl.random.nextDouble(); // draw one because of strange "not-randomness" is the first draw...

			this.stopwatch.beginOperation("mobsim");
			printNote("", "[" + Controler.iteration + "] mobsim starts");
			Gbl.printMemoryUsage();
			runMobSim();
			this.events.printEventsCount();
			this.stopwatch.endOperation("mobsim");
			printNote("", "[" + Controler.iteration + "] mobsim ends");
			Gbl.printMemoryUsage();

			finishIteration(Controler.iteration);

			this.stopwatch.endOperation("iteration");
			this.stopwatch.write(getOutputFilename("stopwatch.txt"));
			printNote("", "[" + Controler.iteration + "] iteration ends");
			Gbl.printRoundTime();
		}
	}

	/**
	 * Setup events- and other algorithms that collect data for later analysis
	 * @param iteration The iteration that will start next
	 */
	protected void setupIteration(final int iteration) {
		this.fireControlerSetupIterationEvent(iteration);
		// TODO [MR] use events.resetHandlers();
		this.travelTimeCalculator.resetTravelTimes();	// reset, so we can collect the new events and build new travel times for the next iteration

		this.eventwriter = new EventWriterTXT(getIterationFilename(Controler.FILENAME_EVENTS));
		this.events.addHandler(this.eventwriter);
		if (this.planScorer == null) {
			if (Gbl.useRoadPricing()) {
				this.planScorer = new EventsToScore(this.population, new RoadPricingScoringFunctionFactory(this.tollCalc, new CharyparNagelScoringFunctionFactory()));
			} else {
				this.planScorer = new EventsToScore(this.population, new CharyparNagelScoringFunctionFactory());
			}
			this.events.addHandler(this.planScorer);
		} else {
			this.planScorer.reset(iteration);
		}

		// collect and average volumes information in iterations *6-*0, e.g. it.6-10, it.16-20, etc
		if ((iteration % 10 == 0) || (iteration % 10 >= 6)) {
			this.volumes.reset(iteration);
			this.events.addHandler(this.volumes);
		}

		this.legTimes.reset(iteration);

		// dump plans every 10th iteration
		if ((iteration % 10 == 0) || (iteration < 3)) {
			printNote("", "dumping all agents' plans...");
			this.stopwatch.beginOperation("dump all plans");
			String outversion = this.config.plans().getOutputVersion();
			PopulationWriter plansWriter = new PopulationWriter(this.population, getIterationFilename(Controler.FILENAME_PLANS), outversion);
			plansWriter.setUseCompression(true);
			plansWriter.write();
			this.stopwatch.endOperation("dump all plans");
			printNote("", "done dumping plans.");
		}
	}

	/**
	 * remove events- and other algorithms and calculate/output analysis results
	 * @param iteration The iteration that just ended
	 */
	protected void finishIteration(final int iteration) {
		log.info("Close event writer.");
		this.events.removeHandler(this.eventwriter);
		this.eventwriter.reset(iteration);

		//
		// score plans and calc average
		//

		PlanAverageScore average = new PlanAverageScore();
		this.planScorer.finish();
		average.run(this.population);
		printNote("S C O R I N G", "[" + iteration + "] the average score is: " + average.getAverage());
		printNote("", "[" + iteration + "] the average plan performance is: " + this.planScorer.getAveragePlanPerformance());

		if ((iteration % 10 == 0) || (iteration % 10 >= 6)) {
			this.events.removeHandler(this.volumes);
			this.linkStats.addData(this.volumes, this.travelTimeCalculator);
		}

		if ((iteration % 10 == 0) && (iteration > this.minIteration)) {
			this.linkStats.writeFile(getIterationFilename(Controler.FILENAME_LINKSTATS));
		}

		// TRIP DURATIONS
		// - write stats to file
		String legStatsFilename = getIterationFilename("tripdurations.txt");
		BufferedWriter legStatsFile = null;
		try {
			legStatsFile = IOUtils.getBufferedWriter(legStatsFilename);
			this.legTimes.writeStats(legStatsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (legStatsFile != null) {
				legStatsFile.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// - print average in log
		printNote("", "[" + iteration + "] average trip duration is: "
				+ (int)this.legTimes.getAverageTripDuration() + " seconds = "
				+ Time.writeTime(this.legTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));
		this.legTimes.reset(iteration);

		if (this.statsManager != null){
			printNote("S T A T S", "running StatsManager ...");
			this.statsManager.run(this.population, iteration);
			if (iteration == this.maxIterations) {
				this.statsManager.writeStats(this.population);
			}
			printNote("","done");
		}
		this.fireControlerFinishIterationEvent(iteration);

		if (iteration % 10 == 0) {
			// linkStats may be used in ControlerFinishIterationEventListeners...
			this.linkStats.reset();
		}
	}


	protected void loadData() {
		loadWorld();
		this.network = loadNetwork();
		loadFacilities();
		this.population = loadPopulation();

		ScoreStats scoreStats = null;
		try {
			// TODO [MR] I "abuse" createLegHistogramPNG here for ScoreStats... create an own flag for this one.
			scoreStats = new ScoreStats(this.population, getOutputFilename("scorestats.txt"), this.createLegHistogramPNG);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (scoreStats != null) {
			this.addControlerListener(scoreStats);
		}
	}

	protected void loadWorld() {
		if (this.config.world().getInputFile() != null) {
			printNote("", "  reading world xml file... ");
			final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
			worldReader.readFile(this.config.world().getInputFile());
			printNote("", "  done");
		} else {
			printNote("","  No World input file given in config.xml!");
		}
	}

	protected NetworkLayer loadNetwork() {
		printNote("", "  creating network layer... ");
		NetworkLayer network = new NetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		printNote("", "  done");

		return network;
	}

//	jhackney
	protected void loadFacilities() {
		if (this.config.facilities().getInputFile() != null) {
			printNote("", "  reading facilities xml file... ");
			this.facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
			new MatsimFacilitiesReader(this.facilities).readFile(this.config.facilities().getInputFile());
			printNote("", "  done");
		} else {
			printNote("","  No Facilities input file given in config.xml!");
		}
	}

	protected Population loadPopulation() {
		Population population = new Population(Population.NO_STREAMING);

		printNote("", "  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPlansReader(population);
		plansReader.readFile(this.config.plans().getInputFile());
		population.printPlansCount();
		printNote("", "  done");

		return population;
	}

	protected void startup() {

		if (Gbl.useRoadPricing()) {
			printNote("", "setting up road pricing support...");
			RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(this.network);
			try {
				rpReader.parse(this.config.getParam("roadpricing", "tollLinksFile"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			this.toll = rpReader.getScheme();
			this.tollCalc = new CalcPaidToll(this.network, this.toll);
			this.events.addHandler(this.tollCalc);
			printNote("", "done.");
		}

		int endTime = (int) (this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30 * 3600); //if no end time is given assume 30 hours
		this.travelTimeCalculator = this.config.controler().getTravelTimeCalculator(this.network, endTime);
		this.events.addHandler(this.travelTimeCalculator);


		if (Gbl.useRoadPricing()) {
			if ((this.toll.getType().equals("distance")) || (this.toll.getType().equals("cordon"))) {
				this.travelCostCalculator = new TollTravelCostCalculator(new TravelTimeDistanceCostCalculator(this.travelTimeCalculator), this.toll);
			} else {
				// use the standard travelCostCalcualtor in case of an area toll
				this.travelCostCalculator = new TravelTimeDistanceCostCalculator(this.travelTimeCalculator);
			}
		} else {
			this.travelCostCalculator = new TravelTimeDistanceCostCalculator(this.travelTimeCalculator);
		}

		/* TODO [MR] linkStats uses ttcalc and volumes, but ttcalc has 15min-steps,
		 * while volumes uses 60min-steps! It works a.t.m., but the traveltimes
		 * in linkStats are the avg. traveltimes between xx.00 and xx.15, and not
		 * between xx.00 and xx.59
		 */
		this.linkStats = new CalcLinkStats(this.network);
		this.volumes = new VolumesAnalyzer(3600, 24*3600-1, this.network);

		this.legTimes = new CalcLegTimes(this.population);
		this.events.addHandler(this.legTimes);

		/* prepare plans for simulation:
		 * - make sure they have exactly one plan selected
		 * - make sure the selected plan was routed
		 */
		printNote("", "  preparing plans for simulation...");
		new PersonPrepareForSim(new PlansCalcRoute(this.network, this.travelCostCalculator, this.travelTimeCalculator), this.network).run(this.population);
		printNote("", "  done");

		this.strategyManager = loadStrategyManager();

		this.statsManager = loadPlanStatsManager();

		this.fireControlerStartupEvent();

	}


	/**
	 * writes necessary information to files and ensures that all files get properly closed
	 *
	 * @param unexpected indicates whether the shutdown was planned (<code>false</code>) or not (<code>true</code>)
	 */
	public final void shutdown(final boolean unexpected) {
		if (this.running) {
			this.running = false;	// this will prevent any further iteration to start

			this.fireControlerShutdownEvent(unexpected);

			if (unexpected) {
				printNote("S H U T D O W N", "unexpected shutdown request");
			}

			if (this.outputDirSetup) {
				printNote("S H U T D O W N", "start shutdown");
				printNote("", "writing and closing all files");

				if (this.eventwriter != null) {
					printNote("", "  trying to close eventwriter...");
					this.eventwriter.closefile();
					printNote("", "  done.");
				}

				printNote("", "  writing plans xml file... ");
				// write the plans into the default output-directory
				PopulationWriter plansWriter = new PopulationWriter(this.population, getOutputFilename("output_plans.xml.gz"),
						this.config.plans().getOutputVersion());
				plansWriter.write();
				printNote("", "  done");

				try {
					printNote("", "  writing facilities xml file... ");
					if (this.config.facilities().getOutputFile() != null) {
						Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
						new FacilitiesWriter(facilities).write();
						printNote("", "  done");
					}
					else {
						printNote("", "  not done, no output file specified in config.xml!");
					}
				}
				catch (Exception e) {
					printNote("", e.getMessage());
				}

				try {
					printNote("", "  writing network xml file... ");
					if (this.config.network().getOutputFile() != null) {
						NetworkWriter network_writer = new NetworkWriter(this.network, getOutputFilename("output_network.xml.gz"));
						network_writer.write();
						printNote("", "  done");
					}
					else {
						printNote("", "  not done, no output file specified in config.xml!");
					}
				}
				catch (Exception e) {
					printNote("", e.getMessage());
				}

				try {
					printNote("", "  writing world xml file... ");
					if (this.config.world().getOutputFile() != null) {
						WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
						world_writer.write();
						printNote("", "  done");
					}
					else {
						printNote("", "  not done, no output file specified in config.xml!");
					}
				}
				catch (Exception e) {
					printNote("", e.getMessage());
				}

				try {
					printNote("", "  writing config xml file... ");
					if (this.config.config().getOutputFile() != null) {
						ConfigWriter config_writer = new ConfigWriter(this.config);
						config_writer.write();
						printNote("", "  done");
					}
					else {
						printNote("", "  not done, no output file specified in config.xml!");
					}
				}
				catch (Exception e) {
					printNote("", e.getMessage());
				}
			}
			if (unexpected) {
				printNote("S H U T D O W N", "unexpected shutdown request completed");
			} else {
				printNote("S H U T D O W N", "shutdown completed");
			}
		}
	}

	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		StrategyManagerConfigLoader.load(this.config, manager, this.network, this.travelCostCalculator, this.travelTimeCalculator);
		return manager;
	}

	protected PlanStatsManager loadPlanStatsManager() {
		String option = this.config.findParam(Controler.STATS_MODULE, Controler.STATS_MANAGER_ACTIVE);
		if ((option != null) && "true".equals(option)) {
			return new PlanStatsManager();
		}

		return null;
	}

	/**
	 * returns the path to a directory where temporary files can be stored.
	 * @return path to a temp-directory.
	 */
	public final static String getTempPath() {
		return Controler.outputPath + "/tmp";
	}

	/**
	 * returns the path to the specified iteration directory. The directory path does not include the trailing '/'
	 * @param iteration the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final static String getIterationPath(final int iteration) {
		return Controler.outputPath + "/" + Controler.DIRECTORY_ITERS + "/it." + iteration;
	}

	/**
	 * returns the path of the current iteration directory. The directory path does not include the trailing '/'
	 * @return path to the current iteration directory
	 */
	public final static String getIterationPath() {
		return getIterationPath(Controler.iteration);
	}

	/**
	 * returns the complete filename to access an iteration-file with the given basename
	 * @param filename the basename of the file to access
	 * @return complete path and filename to a file in a iteration directory
	 */
	public final static String getIterationFilename(final String filename) {
		if (getIteration() == -1) {
			return filename;
		}
		return getIterationPath(Controler.iteration) + "/" + Controler.iteration+ "." + filename;
	}

	/**
	 * returns the complete filename to access an iteration-file with the given basename
	 * @param filename the basename of the file to access
	 * @param iteration the iteration to which the path of the file should point
	 * @return complete path and filename to a file in a iteration directory
	 */
	public final static String getIterationFilename(final String filename, final int iteration) {
		return getIterationPath(iteration) + "/" + iteration + "." + filename;
	}

	/**
	 * returns the complete filename to access a file in the output-directory
	 *
	 * @param filename the basename of the file to access
	 * @return complete path and filename to a file in the output-directory
	 */
	public final static String getOutputFilename(final String filename) {
		return Controler.outputPath + "/" + filename;
	}


	public final static int getIteration() {
		return Controler.iteration;
	}

	public final Population getPopulation() {
		return this.population;
	}

	private final void setupOutputDir() {
		Controler.outputPath = this.config.controler().getOutputDirectory();
		if (Controler.outputPath.endsWith("/")) {
			Controler.outputPath = Controler.outputPath.substring(0, Controler.outputPath.length()-1);
		}

		// make the tmp directory
		File outputDir = new File(Controler.outputPath);
		if (outputDir.exists()) {
			if (outputDir.isFile()) {
				Gbl.errorMsg("Cannot create output directory. " + Controler.outputPath + " is a file and cannot be replaced by a directory.");
			} else {
				if (outputDir.list().length > 0) {
					if (this.overwriteFiles) {
						System.err.println("\n\n\n");
						System.err.println("#################################################\n");
						System.err.println("THE CONTROLER WILL OVERWRITE FILES IN:");
						System.err.println(Controler.outputPath);
						System.err.println("\n#################################################\n");
						System.err.println("\n\n\n");
					} else {
						// the directory is not empty
						// we do not overwrite any files!
						Gbl.errorMsg("The output directory " + Controler.outputPath + " exists already but has files in it! Please delete its content or the directory and start again. We will not delete or overwrite any existing files.");
						// variant: execute a rm -r outputPath_ && mkdir outputPath_
						// but this would only work on *nix-Systems, not Windows.
					}
				}
			}
		} else {
			if (!outputDir.mkdir()) {
				Gbl.errorMsg("The output directory " + Controler.outputPath + " could not be created. Does it's parent directory exist?");
			}
		}

		File tmpDir = new File(getTempPath());
		if (!tmpDir.mkdir() && !tmpDir.exists()) {
			Gbl.errorMsg("The tmp directory " + getTempPath() + " could not be created.");
		}
		File itersDir = new File(Controler.outputPath + "/" + Controler.DIRECTORY_ITERS);
		if (!itersDir.mkdir() && !itersDir.exists()) {
			Gbl.errorMsg("The iterations directory " + (Controler.outputPath + "/" + Controler.DIRECTORY_ITERS) + " could not be created.");
		}
		this.outputDirSetup  = true;
	}

	private final void makeIterationPath(final int iteration) {
		if (!(new File(getIterationPath(iteration)).mkdir())) {
			log.warn("Could not create iteration directory " + getIterationPath(iteration) + ". (Directory may already exist.)");
		}
	}

	/**
	 * Sets whether the Controler is allowed to overwrite files in the output
	 * directory or not. <br>
	 * When starting, the Controler can check that the output directory is empty
	 * or does not yet exist, so no files will be overwritten (default setting).
	 * While useful in a productive environment, this security feature may be
	 * interfering in testcases or while debugging. <br>
	 * <strong>Use this setting with caution, as it can result in data loss!</strong>
	 *
	 * @param overwrite
	 *          whether files and directories should be overwritten (true) or not
	 *          (false)
	 */
	public final void setOverwriteFiles(final boolean overwrite) {
		this.overwriteFiles = overwrite;
	}

	/**
	 * Returns whether the Controler is currently allowed to overwrite files in
	 * the output directory.
	 *
	 * @return true if the Controler is currently allowed to overwrite files in
	 *         the output directory, false if not.
	 */
	public final boolean getOverwriteFiles() {
		return this.overwriteFiles;
	}

	/** Specifies whether the leg histogram should also be generated as a PNG graphics. The creation uses
	 * a small amount of time, but large enough to slow down small scenarios like test cases and  equil-net.
	 *
	 * @param createPng true if a PNG graphics should be generated each iteration.
	 */
	public final void setCreateLegHistogramPNG(final boolean createPng) {
		this.createLegHistogramPNG = createPng;
	}

	/**
	 * an internal routine to generated some (nicely?) formatted output. This helps that status output
	 * looks about the same every time output is written.
	 *
	 * @param header the header to print, e.g. a module-name or similar. If empty <code>""</code>, no header will be printed at all
	 * @param action the status message, will be printed together with a timestamp
	 */
	protected final void printNote(final String header, final String action) {
		if (!header.equals("")) {
			log.info("");
			log.info("===============================================================");
			log.info("== " + header);
			log.info("===============================================================");
		}
		if (!action.equals("")) {
			log.info("== " + action);
		}
		if (!header.equals("")) {
			log.info("");
		}
	}

	/**
	 * Add a ControlerListener to the Controler instance
	 * @param l
	 */
	public void addControlerListener(final ControlerListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (ControlerListener.class.isAssignableFrom(interfaces[i])) {
				this.listenerList.add(interfaces[i], l);
			}
		}
	}
	/**
	 * Removes a ControlerListener from the Controler instance
	 * @param l
	 */
	public void removeControlerListener(final ControlerListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (ControlerListener.class.isAssignableFrom(interfaces[i])) {
				this.listenerList.remove(interfaces[i], l);
			}
		}
	}

	/**
	 * Notifies all ControlerListeners
	 */
	protected void fireControlerStartupEvent() {
    ControlerStartupListener[] listener = this.listenerList.getListeners(ControlerStartupListener.class);
    ControlerStartupEvent event = new ControlerStartupEvent(this);
    for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyStartup(event);
    }
	}

	/**
	 * Notifies all ControlerListeners
	 */
	protected void fireControlerShutdownEvent(final boolean unexpected) {
		ControlerShutdownListener[] listener = this.listenerList.getListeners(ControlerShutdownListener.class);
		ControlerShutdownEvent event = new ControlerShutdownEvent(this, unexpected);
    for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyShutdown(event);
    }
	}

	/**
	 * Notifies all ControlerSetupIterationListeners
	 * @param iteration
	 */
	protected void fireControlerSetupIterationEvent(final int iteration) {
		ControlerSetupIterationListener[] listener = this.listenerList.getListeners(ControlerSetupIterationListener.class);
    ControlerSetupIterationEvent event = new ControlerSetupIterationEvent(this, iteration);
		for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyIterationSetup(event);
    }
	}

	/**
	 * Notifies all ControlerFinishIterationListeners
	 * @param iteration
	 */
	private void fireControlerFinishIterationEvent(final int iteration) {
		ControlerFinishIterationListener[] listener = this.listenerList.getListeners(ControlerFinishIterationListener.class);
		ControlerFinishIterationEvent event = new ControlerFinishIterationEvent(this, iteration);
		for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyIterationFinished(event);
    }
	}

	/**
	 * @return the minimum iteration of the controler
	 */
	public int getMinimumIteration() {
		return this.minIteration;
	}

	/**
	 * @return the maximum iteration of the controler
	 */
	public int getMaximumIteration() {
		return this.maxIterations;
	}

	/**
	 * @return the CalcLinkStats object of the controler
	 */
	public CalcLinkStats getCalcLinkStats() {
		return this.linkStats;
	}

	/**
	 * @return the network loaded
	 */
	public NetworkLayer getNetwork() {
		return this.network;
	}

	// main-routine, where it all starts...
	public static void main(final String[] args) {
		final Controler controler = new Controler();

		controler.run(args);
		System.exit(0);
	}
}

