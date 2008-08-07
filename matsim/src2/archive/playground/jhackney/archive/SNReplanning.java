/* *********************************************************************** *
 * project: org.matsim.*
 * SNReplanning.java
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

package playground.jhackney.deprecated;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
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
import org.matsim.population.Knowledge;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAverageScore;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.socialnetworks.interactions.NonSpatialInteractor;
import org.matsim.socialnetworks.interactions.SocializingOpportunity;
import org.matsim.socialnetworks.interactions.SpatialSocialOpportunityTracker;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.socialnetworks.statistics.SocialNetworkStatistics;
import org.matsim.trafficmonitoring.TravelTimeCalculatorArray;
import org.matsim.utils.misc.Time;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;

import playground.jhackney.scoring.SNScoringFunctionFactory01;

public class SNReplanning  {

	public static final String CONFIG_MODULE = "controler";
	public static final String CONFIG_OUTPUT_DIRECTORY = "outputDirectory";
	public static final String CONFIG_FIRST_ITERATION = "firstIteration";
	public static final String CONFIG_LAST_ITERATION = "lastIteration";

	/** The Config instance the Controler uses. */
	protected Config config = null;

	public static final String FILENAME_EVENTS = "events.txt";
	public static final String FILENAME_PLANS = "plans.xml";
	public static final String FILENAME_LINKSTATS = "linkstats.att";

	private static final String DIRECTORY_ITERS = "ITERS";
	private static final String DIRECTORY_SN = "socialnets";
	public static String SOCNET_OUT_DIR = null;

	protected final Events events = new Events();
	protected Population population = null;

	private boolean running = false;
	private StrategyManager strategyManager = null;
	protected NetworkLayer network = null;
	protected Facilities facilities = null;
	private TravelTimeI travelTimeCalculator = null;
	private TravelCostI travelCostCalculator=null;
	private static String outputPath = null;
	private static int iteration = -1;

	private EventsToScore planScorer = null;
	private CharyparNagelScoringFunction defaultScorer = null;
	private EventWriterTXT eventwriter = null;

	//private CalcVehicleToll tollCalc = null;
	private CalcPaidToll tollCalc=null;
	private CalcLinkStats linkStats = null;
	private CalcLegTimes legTimes = null;
	private VolumesAnalyzer volumes = null;

	private boolean overwriteFiles = true;
	private int minIteration;
	private int maxIterations;
//	---------------------- social network variables ---------------- //
//	-- these are here because they are set up in setup and needed again in iterations --//

	SocialNetwork snet;
	SocialNetworkStatistics snetstat;
	PajekWriter1 pjw;
	NonSpatialInteractor plansInteractorNS;//non-spatial (not observed, ICT)
	SpatialInteractor plansInteractorS;//spatial (face to face)
	int max_sn_iter;
	String [] infoToExchange;//type of info for non-spatial exchange is read in
	public static String activityTypesForEncounters[]={"home","work","shop","education","leisure"};

	SpatialSocialOpportunityTracker gen2 = new SpatialSocialOpportunityTracker();
	Collection<SocializingOpportunity> socialEvents=null;

	boolean hackSocNets = true;

//	Variables for allocating the spatial meetings among different types of activities
	double fractionS[];
	HashMap<String,Double> rndEncounterProbs= new HashMap<String,Double>();
//	New variables for replanning
	int replan_interval;

//	-------------------- end social network variables --------------------//

	/** Describes whether the output directory is correctly set up and can be used. */
	private boolean outputDirSetup = false;

	private final static Logger log = Logger.getLogger(SNReplanning.class);

	public SNReplanning() {
		super();
	}

	public final void run(String[] args) {
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

		this.minIteration = Integer.parseInt(this.config.getParam(CONFIG_MODULE, CONFIG_FIRST_ITERATION));
//		this.maxIterations = Integer.parseInt(this.config.getParam(CONFIG_MODULE, CONFIG_LAST_ITERATION));
		this.maxIterations = 10;


		setupOutputDir(); // make sure all required directories exist
		loadData(); // only reading data: network, plans, facilities, events, ...
		startup(); // init some "global objects", prepare data for simulation, ...



//		JKH set up social network and its IO around the mobility simulation iterations

////		Set up social network
		System.out.println("----------Initialization of social network -------------------------------------");
		snsetup();
		System.out.println(" Beginning the relaxation of social connections...");
		for (int snIter = 1; snIter <= this.max_sn_iter; snIter++) {
			interact(snIter);
		}
		System.out.println("----------Closing social network statistic files and wrapping up ---------------");
		this.snetstat.closeFiles();
		snwrapup();

//		JKH end



		shutdown(false);

		printNote("M A T S I M - C O N T R O L E R", "exit");
	}
	private void snwrapup(){
		JUNGPajekNetWriterWrapper pnww = new JUNGPajekNetWriterWrapper(outputPath,this.snet, this.population);
		pnww.write();

		System.out.println(" Writing the statistics of the final social network to Output Directory...");

		SocialNetworkStatistics snetstatFinal=new SocialNetworkStatistics();
		snetstatFinal.openFiles(outputPath);
		snetstatFinal.calculate(this.max_sn_iter, this.snet, this.population);

		System.out.println(" ... done");
		snetstatFinal.closeFiles();
	}
	private void snsetup() {

//		Config config = Gbl.getConfig();

		this.max_sn_iter = Integer.parseInt(this.config.socnetmodule().getNumIterations());
		this.replan_interval = Integer.parseInt(this.config.socnetmodule().getRPInt());
		String rndEncounterProbString = this.config.socnetmodule().getFacWt();
		String interactorNSFacTypesString = this.config.socnetmodule().getXchange();
		this.infoToExchange = getFacTypes(interactorNSFacTypesString);
		this.fractionS = getActivityTypeAllocation(rndEncounterProbString);
		this.rndEncounterProbs = getActivityTypeAllocationMap(activityTypesForEncounters, rndEncounterProbString);

		// TODO Auto-generated method stub
		System.out.println(" Instantiating the Pajek writer ...");

		pjw = new PajekWriter1(SOCNET_OUT_DIR, this.facilities);
		System.out.println("... done");

		System.out.println(" Initializing the social network ...");
		this.snet = new SocialNetwork(this.population);
		System.out.println("... done");

		System.out.println(" Calculating the statistics of the initial social network)...");
		this.snetstat=new SocialNetworkStatistics();
		this.snetstat.openFiles();
		this.snetstat.calculate(0, this.snet, this.population);
		System.out.println(" ... done");

		System.out.println(" Writing out the initial social network ...");
		pjw.write(this.snet.getLinks(), this.population, 0);
		System.out.println("... done");

		System.out.println(" Setting up the NonSpatial interactor ...");
		this.plansInteractorNS=new NonSpatialInteractor(this.snet);
		System.out.println("... done");

		System.out.println(" Setting up the Spatial interactor ...");
		this.plansInteractorS=new SpatialInteractor(this.snet);
		System.out.println("... done");
	}

	/**
	 * The interact method runs the mobility simulation within the iterating social network
	 * simulation.
	 *
	 */
	private void interact(int snIter) {

		System.out.println("----------- Begin social interaction iteration "+snIter+" -------------");

		System.out.println(" Spatial interactions...");
		if(total_spatial_fraction(this.fractionS)>0){
			if(snIter==1){
//				snsetup();
				System.out.println("  Generating [Spatial] socializing opportunities ...");
				System.out.println("   Mapping which agents were doing what, where, and when");
				// Create the social opportunities from plans (updated each time plans change)
				// OK to initialize from plans but do this from events if events != null!
				this.socialEvents = this.gen2.generate(this.population);
				System.out.println("...finished.");
			}
			// Agents interact at the social opportunities
			System.out.println("  Agents interact at the social opportunities ...");
			this.plansInteractorS.interact(this.socialEvents, this.rndEncounterProbs, snIter);
		}else{
			System.out.println("     (none)");
		}
		System.out.println(" ... Spatial interactions done\n");

		System.out.println(" Removing social links ...");
		this.snet.removeLinks(snIter);
		System.out.println(" ... done");

		System.out.println(" Non-Spatial interactions ...");
		for (int ii = 0; ii < this.infoToExchange.length; ii++) {
			String facTypeNS = this.infoToExchange[ii];

			//	Geographic Knowledge about all types of places is exchanged
			if (!facTypeNS.equals("none")) {
				System.out.println("  Geographic Knowledge about all types of places is being exchanged ...");
				this.plansInteractorNS.exchangeGeographicKnowledge(facTypeNS, snIter);
			}
		}

		// Exchange of knowledge about people
		double fract_intro=Double.parseDouble(this.config.socnetmodule().getTriangles());
		if (fract_intro > 0) {
			System.out.println("  Knowledge about other people is being exchanged ...");
			this.plansInteractorNS.exchangeSocialNetKnowledge(snIter);
		}

		System.out.println("  ... done");

//		Altering plans


		System.out.println(" Replan every "+this.replan_interval+"th iteration of the social network.");

		if (((snIter > 1) && (snIter % this.replan_interval == 0))||(this.replan_interval==1)) {
			System.out.println("  Begin replanning snIter = "+snIter+" ...");
			doReplanningIterations(snIter);
			System.out.println("  Updating [Spatial] socializing opportunities to changed plans for iteration " + snIter + "...");
			this.socialEvents=this.gen2.generate(this.population);
			System.out.println("... finished.");
		}

		System.out.println(" Calculating and reporting network statistics ...");
		this.snetstat.calculate(snIter, this.snet, this.population);
		System.out.println(" ... done");

		System.out.println(" Writing out social network for iteration " + snIter + " ...");
		pjw.write(this.snet.getLinks(), this.population, snIter);
		System.out.println(" ... done");
	}


	private double total_spatial_fraction(double[] fractionS2) {
//		See if we use spatial interaction at all: sum of these must > 0 or else no spatial
//		interactions take place
		double total_spatial_fraction=0;
		for (int jjj = 0; jjj < fractionS2.length; jjj++) {
			total_spatial_fraction = total_spatial_fraction + fractionS2[jjj];
		}
		return total_spatial_fraction;
	}

	protected void runMobSim() {
		SimulationTimer.setTime(0);

		String externalMobsim = this.config.findParam("simulation", "externalExe");
		if (externalMobsim == null) {
			System.out.println("Write a Simulation object that returns the unloaded network travel times");
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
	/**
	 * A method for decyphering the config codes. Part of configuration
	 * reader. Replace eventually with a routine that runs all of the
	 * facTypes but uses a probability for each one, summing to 1.0. Change
	 * the interactors accordingly.
	 *
	 * @param longString
	 * @return
	 */
	private String[] getFacTypes(String longString) {
		// TODO Auto-generated method stub
		String patternStr = ",";
		String[] s;
		log.info("!!add keyword\"any\" and a new interact method to exchange info of any factility types (compatible with probabilities)");
		if (longString.equals("all-p")) {
			s = new String[5];
			s[0] = "home";
			s[1] = "work";
			s[2] = "education";
			s[3] = "leisure";
			s[4] = "shop";
		} else if (longString.equals("all+p")) {
			s = new String[6];
			s[0] = "home";
			s[1] = "work";
			s[3] = "education";
			s[4] = "leisure";
			s[5] = "shop";
			s[6] = "person";
		} else {
			s = longString.split(patternStr);
		}
		for (int i = 0; i < s.length; i++) {
			// if(s[i]!="home"&&s[i]!="work"&&s[i]!="education"&&s[i]!="leisure"&&s[i]!="shop"&&s[i]!="person"&&s[i]!="none"){
			if (!s[i].equals("home") && !s[i].equals("work") && !s[i].equals("education") && !s[i].equals("leisure")
					&& !s[i].equals("shop") && !s[i].equals("person") && !s[i].equals("none")) {
				System.out.println(this.getClass() + ":" + s[i]);
				Gbl.errorMsg("Error on type of info to exchange. Check config file. Use commas with no spaces");
			}
		}
		return s;
	}

	private double[] getActivityTypeAllocation(String longString) {
		String patternStr = ",";
		String[] s;
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if((w[i]<0.)||(w[i]>1.)){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return w;
	}
	private HashMap<String,Double> getActivityTypeAllocationMap(String[] types, String longString) {
		String patternStr = ",";
		String[] s;
		HashMap<String,Double> map = new HashMap<String,Double>();
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if((w[i]<0.)||(w[i]>1.)){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
			map.put(types[i],w[i]);
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return map;
	}

	private final void doReplanningIterations(int snIter) {
//		The scoring is based on Events.
		Gbl.startMeasurement();
		for (iteration = this.minIteration; iteration <= this.maxIterations; iteration++) {
			printNote("I T E R A T I O N   " + iteration, "[" + iteration + "] iteration begins");

			makeSNIterationPath(snIter);
			makeSNIterationPath(iteration, snIter);

			// reset random seed every iteration so we can more easily resume runs
			Gbl.random.setSeed(this.config.global().getRandomSeed() + iteration);
			Gbl.random.nextDouble(); // draw one because of strange "not-randomness" is the first draw...

			if (this.tollCalc != null) {		// roadPricing only
				this.tollCalc.reset(iteration);
			}

			//
			// generate new plans for some percentage of population
			//
			if (snIter > 1) {
				printNote("R E P L A N N I N G   " + iteration, "[" + iteration + "] running strategy modules begins");
				this.strategyManager.run(this.population, iteration);
				printNote("R E P L A N N I N G   " + iteration, "[" + iteration + "] running strategy modules ends");
			}

			setupIteration(iteration, snIter);

			// reset random seed again before mobsim, as we do not know if strategy modules ran and if they used random numbers.
			Gbl.random.setSeed(this.config.global().getRandomSeed() + iteration);
			Gbl.random.nextDouble(); // draw one because of strange "not-randomness" is the first draw...

			printNote("", "[" + iteration + "] mobsim starts");
			System.out.println("## Test of initial demand generation with Euclidean plan length minimization, no MobSim is run");
			runMobSim();
			printNote("", "[" + iteration + "] mobsim ends");

			finishIteration(iteration, snIter);

			printNote("", "[" + iteration + "] iteration ends");
			Gbl.printRoundTime();
		}// for iterations (old iterations)
	}

	/**
	 * Setup events- and other algorithms that collect data for later analysis
	 * @param iteration The iteration that will start next
	 */
	protected void setupIteration(int iteration, int snIter) {

		// TODO [MR] use events.resetHandlers();
		((TravelTimeCalculatorArray)this.travelTimeCalculator).resetTravelTimes();	// reset, so we can collect the new events and build new travel times for the next iteration

		this.eventwriter = new EventWriterTXT(getIterationFilename(FILENAME_EVENTS, snIter));
		this.events.addHandler(this.eventwriter);
		if (this.planScorer == null) {
//			if (Gbl.useRoadPricing()) {
//			this.planScorer = new EventsToScore(this.population, new RoadPricingScoringFunctionFactory(this.tollCalc));
//			}
			if (this.hackSocNets) {
				System.out.println("### USING SOCIAL NETS SCORER");
				this.planScorer = new EventsToScore(this.population, new SNScoringFunctionFactory01());
			}else {
				this.planScorer = new EventsToScore(this.population, new CharyparNagelScoringFunctionFactory());
			}
			this.events.addHandler(this.planScorer);
		} else {
			this.planScorer.reset(iteration);
		}

		// collect and average volumes information in iterations *6-*0, e.g. it.6-10, it.16-20, etc
		if ((iteration % 10 == 0) || (iteration % 10 >= (this.minIteration + 6))) {
			this.volumes.reset(iteration);
			this.events.addHandler(this.volumes);
		}

		this.legTimes.reset(iteration);

		// dump plans every 10th iteration
		if ((iteration % 10 == 0) || (iteration < 3)) {
			printNote("", "dumping all agents' plans...");
			String outversion = this.config.plans().getOutputVersion();
			PopulationWriter plansWriter = new PopulationWriter(this.population, getIterationFilename(FILENAME_PLANS, snIter), outversion);
			plansWriter.setUseCompression(true);
			plansWriter.write();
			printNote("", "done dumping plans.");
		}
	}

	/**
	 * remove events- and other algorithms and calculate/output analysis results
	 * @param iteration The iteration that just ended
	 */
	protected void finishIteration(int iteration, int snIter) {
		System.out.println("close event writer");
		this.events.removeHandler(this.eventwriter);
		this.eventwriter.reset(iteration);

		//
		// score plans and calc average
		//

		PlanAverageScore average = new PlanAverageScore();
		this.planScorer.finish();
		average.run(this.population);
		printNote("S C O R I N G", "[" + iteration + "] the average score is: " + average.getAverage());

		if ((iteration % 10 == 0) || (iteration % 10 >= (this.minIteration + 6))) {
			this.events.removeHandler(this.volumes);
			this.linkStats.addData(this.volumes, this.travelTimeCalculator);
		}

//		if (iteration % 10 == 0 && iteration > this.minIteration) {
//		new LinkAttributesWriter(linkStats.getLinkAttributes()).writefile(getIterationFilename(FILENAME_LINKSTATS, snIter));
//		this.linkStats.reset();
//		}

		// TRIP DURATIONS
		// - write stats to file
		String legStatsFilename = getIterationFilename("tripdurations.txt", snIter);
		BufferedWriter legStatsFile;
		try {
			legStatsFile = new BufferedWriter(new FileWriter(legStatsFilename));
			this.legTimes.writeStats(legStatsFile);
			legStatsFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// - print average in log
		printNote("", "[" + iteration + "] average trip duration is: "
				+ (int)this.legTimes.getAverageTripDuration() + " seconds = "
				+ Time.writeTime(this.legTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));
		this.legTimes.reset(iteration);

	}

	protected void loadData() {
//		loadWorld();
		this.facilities = loadFacilities();
		this.network = loadNetwork();
		this.population = loadPopulation();
		// Stitch together the world
		//Gbl.getWorld().complete();
		new WorldBottom2TopCompletion().run(Gbl.getWorld());

		System.out.println(" Initializing agent knowledge ...");
		initializeKnowledge(this.population);
		System.out.println("... done");
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
		// - read network: which buildertype??
		printNote("", "  creating network layer... ");
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		printNote("", "  done");

		return network;
	}

	protected Facilities loadFacilities() {
		if (this.config.facilities().getInputFile() != null) {
			printNote("", "  reading facilities xml file... ");
			this.facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
			new MatsimFacilitiesReader(this.facilities).readFile(this.config.facilities().getInputFile());
			printNote("", "  done");
		} else {
			printNote("","  No Facilities input file given in config.xml!");
		}
		return this.facilities;
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

	private final void startup() {

		if (Gbl.useRoadPricing()) {
			System.out.println("setting up road pricing support...");
			RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(this.network);
			try {
				rpReader.parse(this.config.getParam("roadpricing", "tollLinksFile"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			RoadPricingScheme scheme = rpReader.getScheme();
			this.tollCalc = new CalcPaidToll(this.network, scheme);
			this.events.addHandler(this.tollCalc);
			System.out.println("done.");
		}

		TravelTimeCalculatorArray travelTimeCalculator = new TravelTimeCalculatorArray(this.network, 15*60); // 15min bins
		this.events.addHandler(travelTimeCalculator);
		this.travelTimeCalculator = travelTimeCalculator;

//		if (Gbl.useRoadPricing()) {
//		this.travelCostCalculator = new TollTravelTimeCalculator(network, 15*60, this.tollCalc);
//		}

//		if (Gbl.hackSocNet){
		//this.travelCostCalculator = new SNTTDCostCalculator(travelTimeCalculator);
		System.out.println("### PUT IN SOCIAL NETWORK-RELEVANT DISTANCE/TIME CALCULATOR HERE");
//		}else {
		this.travelCostCalculator = new TravelTimeDistanceCostCalculator(travelTimeCalculator);
//		}

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
	}

	/**
	 * writes necessary information to files and ensures that all files get properly closed
	 *
	 * @param unexpected indicates whether the shutdown was planned (<code>false</code>) or not (<code>true</code>)
	 */
	protected final void shutdown(boolean unexpected) {
//		if (running) {
//			running = false;	// this will prevent any further iteration to start
//
//			if (unexpected) {
//				printNote("S H U T D O W N", "unexpected shutdown request");
//			}
//
//			if (this.outputDirSetup) {
//				printNote("S H U T D O W N", "start shutdown");
//				printNote("", "writing and closing all files");
//
//				if (eventwriter != null) {
//					printNote("", "  trying to close eventwriter...");
//					eventwriter.closefile();
//					printNote("", "  done.");
//				}
//
//				printNote("", "  writing plans xml file... ");
//				PlansWriter plansWriter = new PlansWriter(population);
//				plansWriter.write();
//				printNote("", "  done");
//				try {
//					printNote("", "  writing facilities xml file... ");
//					if (this.config.facilities().getOutputFile() != null) {
//						Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
//						new FacilitiesWriter(facilities).write();
//						printNote("", "  done");
//					} else printNote("", "  not done, no output file specified in config.xml!");
//				}
//				catch (Exception e) {
//					printNote("", e.getMessage());
//				}
//
//				try {
//					printNote("", "  writing network xml file... ");
//					if (this.config.network().getOutputFile() != null) {
//						NetworkWriter network_writer = new NetworkWriter(network);
//						network_writer.write();
//						printNote("", "  done");
//					} else printNote("", "  not done, no output file specified in config.xml!");
//				}
//				catch (Exception e) {
//					printNote("", e.getMessage());
//				}
//
//				try {
//					printNote("", "  writing world xml file... ");
//					if (this.config.world().getOutputFile() != null) {
//						WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
//						world_writer.write();
//						printNote("", "  done");
//					} else printNote("", "  not done, no output file specified in config.xml!");
//				}
//				catch (Exception e) {
//					printNote("", e.getMessage());
//				}
//				try {
//					printNote("", "  writing config xml file... ");
//					if (this.config.config().getOutputFile() != null) {
//						ConfigWriter config_writer = new ConfigWriter(this.config);
//						config_writer.write();
//						printNote("", "  done");
//					} else printNote("", "  not done, no output file specified in config.xml!");
//				}
//				catch (Exception e) {
//					printNote("", e.getMessage());
//				}
//			}
//			if (unexpected) {
//				printNote("S H U T D O W N", "unexpected shutdown request completed");
//			} else {
//				printNote("S H U T D O W N", "shutdown completed");
//			}
//		}
		if (this.running) {
			this.running = false;	// this will prevent any further iteration to start

			//this.fireControlerShutdownEvent(unexpected);

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
	/**
	 * This is a test StrategyManager to see if the replanning works within the social network iterations.
	 * @author jhackney
	 * @return
	 */
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();

		String maxvalue = this.config.findParam("strategy", "maxAgentPlanMemorySize");
		manager.setMaxPlansPerAgent(Integer.parseInt(maxvalue));

		// Best-scoring plan chosen each iteration
		PlanStrategy strategy1 = new PlanStrategy(new BestPlanSelector());

		// Social Network Facility Exchange test
		System.out.println("### NOTE THAT FACILITY SWITCHER IS HARD-CODED TO RANDOM SWITCHING OF FACILITIES FROM KNOWLEDGE");
		System.out.println("### NOTE THAT YOU SHOULD EXCHANGE KNOWLEDGE BASED ON ITS VALUE");
		strategy1.addStrategyModule(new SNFacilitySwitcher());


		// Social Network Facility Exchange for all agents
		manager.addStrategy(strategy1, 1.0);
		return manager;
	}

	/**
	 * returns the path to a directory where temporary files can be stored.
	 * @return path to a temp-directory.
	 */
	public final static String getTempPath() {
		return outputPath + "/tmp";
	}
	/**
	 * returns the path to the specified iteration directory,
	 * including social network iteration. The directory path does not include the trailing '/'
	 * @param iteration the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final static String getSNIterationPath(int iteration, int sn_iter) {
		return outputPath + "/" + DIRECTORY_ITERS + "/"+sn_iter + "/it." + iteration;
	}
	/**
	 * returns the path to the specified social network iteration directory. The directory path does not include the trailing '/'
	 * @param iteration the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final static String getSNIterationPath(int snIter) {
		return outputPath + "/" + DIRECTORY_ITERS + "/"+snIter;
	}
	/**
	 * returns the path to the specified iteration directory. The directory path does not include the trailing '/'
	 * @param iteration the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final static String getIterationPath(int iteration) {
		return outputPath + "/" + DIRECTORY_ITERS + "/it." + iteration;
	}

	/**
	 * returns the path of the current iteration directory. The directory path does not include the trailing '/'
	 * @return path to the current iteration directory
	 */
	public final static String getIterationPath() {
		return getIterationPath(iteration);
	}

	/**
	 * returns the complete filename to access an iteration-file with the given basename
	 * @param filename the basename of the file to access
	 * @return complete path and filename to a file in a iteration directory
	 */
	public final static String getIterationFilename(String filename) {
		if (getIteration() == -1) return filename;
		return getIterationPath(iteration) + "/" + iteration+ "." + filename;
	}

	/**
	 * returns the complete filename to access an iteration-file with the given basename
	 * @param filename the basename of the file to access
	 * @param iteration the iteration to which the path of the file should point
	 * @return complete path and filename to a file in a iteration directory
	 */
	public final static String getIterationFilename(String filename, int iter) {
//		return getIterationPath(iteration) + "/" + iteration + "." + filename;
		return getSNIterationPath(iteration,iter) + "/" + iteration + "." + filename;
	}

	/**
	 * returns the complete filename to access a file in the output-directory
	 *
	 * @param filename the basename of the file to access
	 * @return complete path and filename to a file in the output-directory
	 */
	public final static String getOutputFilename(String filename) {
		return outputPath + "/" + filename;
	}


	public final static int getIteration() {
		return iteration;
	}

	public final Population getPopulation() {
		return this.population;
	}

	private final void setupOutputDir() {
		outputPath = this.config.getParam(CONFIG_MODULE, CONFIG_OUTPUT_DIRECTORY);
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length()-1);
		}

		// make the tmp directory
		File outputDir = new File(outputPath);
		if (outputDir.exists()) {
			if (outputDir.isFile()) {
				Gbl.errorMsg("Cannot create output directory. " + outputPath + " is a file and cannot be replaced by a directory.");
			} else {
				if (outputDir.list().length > 0) {
					if (this.overwriteFiles) {
						System.err.println("\n\n\n");
						System.err.println("#################################################\n");
						System.err.println("THE CONTROLER WILL OVERWRITE FILES IN:");
						System.err.println(outputPath);
						System.err.println("\n#################################################\n");
						System.err.println("\n\n\n");
					} else {
						// the directory is not empty
						// we do not overwrite any files!
						Gbl.errorMsg("The output directory " + outputPath + " exists already but has files in it! Please delete its content or the directory and start again. We will not delete or overwrite any existing files.");
						// variant: execute a rm -r outputPath_ && mkdir outputPath_
						// but this would only work on *nix-Systems, not Windows.
					}
				}
			}
		} else {
			if (!outputDir.mkdir()) {
				Gbl.errorMsg("The output directory " + outputPath + " could not be created. Does its parent directory exist?");
			}
		}

		File tmpDir = new File(getTempPath());
		if (!tmpDir.mkdir() && !tmpDir.exists()) {
			Gbl.errorMsg("The tmp directory " + getTempPath() + " could not be created.");
		}
		File itersDir = new File(outputPath + "/" + DIRECTORY_ITERS);
		if (!itersDir.mkdir() && !itersDir.exists()) {
			Gbl.errorMsg("The iterations directory " + (outputPath + "/" + DIRECTORY_ITERS) + " could not be created.");
		}
		SOCNET_OUT_DIR = outputPath + "/"+DIRECTORY_SN;
		File snDir = new File(SOCNET_OUT_DIR);
		if (!snDir.mkdir() && !snDir.exists()) {
			Gbl.errorMsg("The iterations directory " + (outputPath + "/" + DIRECTORY_SN) + " could not be created.");
		}
		this.outputDirSetup  = true;
	}

	private final void makeIterationPath(int iteration) {
		new File(getIterationPath(iteration)).mkdir();
	}
	private final void makeSNIterationPath(int iteration) {
		new File(getSNIterationPath(iteration)).mkdir();
	}
	private final void makeSNIterationPath(int iteration, int snIter) {
		System.out.println(getSNIterationPath(iteration, snIter));
		File iterationOutFile= new File(getSNIterationPath(iteration, snIter));
		iterationOutFile.mkdir();
//		if (!iterationOutFile.mkdir()) {
//		Gbl.errorMsg("The output directory " + iterationOutFile + " could not be created. Does its parent directory exist?");
//		}
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
	public final void setOverwriteFiles(boolean overwrite) {
		this.overwriteFiles = overwrite;
	}

	/**
	 * returns whether the Controler is currently allowed to overwrite files in
	 * the output directory.
	 *
	 * @return true if the Controler is currently allowed to overwrite files in
	 *         the output directory, false if not.
	 */
	public final boolean getOverwriteFiles() {
		return this.overwriteFiles;
	}

	/**
	 * an internal routine to generated some (nicely?) formatted output. This helps that status output
	 * looks about the same every time output is written.
	 *
	 * @param header the header to print, e.g. a module-name or similar. If empty <code>""</code>, no header will be printed at all
	 * @param action the status message, will be printed together with a timestamp
	 */
	protected final void printNote(String header, String action) {
		if (header != "") {
			System.out.println();
			System.out.println("===============================================================");
			System.out.println("== " + header);
			System.out.println("===============================================================");
		}
		if (action != "") {
			System.out.println("== " + action + " at " + (new Date()) );
		}
		if (header != "") {
			System.out.println();
		}
	}
	void initializeKnowledge( Population plans ){

		// Knowledge is already initialized in some plans files
		// Map agents' knowledge (Activities) to their experience in the plans (Acts)

		for( Person person : plans.getPersons().values() ){

			Knowledge k = person.getKnowledge();
			if(k ==null){
				k = person.createKnowledge("created by " + this.getClass().getName());
			}
			// Initialize knowledge to the facilities that are in all initial plans
			Iterator<Plan> piter=person.getPlans().iterator();
			while (piter.hasNext()){
				Plan plan = piter.next();
				k.map.matchActsToActivities(plan);
			}
		}
	}

	// main-routine, where it all starts...
	public static void main(String[] args) {
		final SNReplanning controler = new SNReplanning();

		Runtime run = Runtime.getRuntime();
		run.addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				controler.shutdown(true);
			}
		} );

		controler.run(args);
		System.exit(0);
	}
}
