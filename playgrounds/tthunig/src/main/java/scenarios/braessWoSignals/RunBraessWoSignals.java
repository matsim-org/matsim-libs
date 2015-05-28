package scenarios.braessWoSignals;

import java.io.File;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.analysis.AnalyzeBraessSimulation;

/**
 * Class to run a simulation of the braess scenario without signals. It also
 * analyzes the simulation with help of AnalyseBraessSimulation.java.
 * 
 * @author tthunig
 * 
 */
public class RunBraessWoSignals {

	private static final Logger log = Logger
			.getLogger(RunBraessWoSignals.class);
	
	private String inputDir;
	private String basicConfigFile;
	private String basicNetworkFile;
	private String plansFile;
	private String outputDir;
	private int iterations;
	private boolean writeEventsForAllIts;
	private boolean useLanes;
	private String lanesInfo;
	private long capMain;
	private long capFirstLast;
	private double[] linkTTs;
	private double propChangeExpBeta;
	private double propReRoute;
	private double propKeepLast;
	private double propSelectRandom;
	private double propSelectExpBeta;
	private double propBestScore;
	private double brainExpBeta;
	private int travelTimeBinSize;
	private double sigma;
	private double monetaryDistanceCostRate;
	private boolean enableLinkToLinkRouting;


	public RunBraessWoSignals(String inputDir, String basicConfig,
			String basicNetwork, String plansFile, String outputDir,
			int iterations, boolean writeEventsForAllIts, double[] linkTTs,
			boolean useLinkLength200, String lanesInfo,
			long capMain, long capFirstLast, double propChangeExpBeta,
			double propReRoute, double propKeepLast, double propSelectRandom,
			double propSelectExpBeta, double propBestScore,
			double brainExpBeta, int ttBinSize, double sigma,
			double monetaryDistanceCostRate, boolean enableLinkToLinkRouting) {

		this.inputDir = inputDir;
		this.basicConfigFile = basicConfig;
		this.basicNetworkFile = basicNetwork;
		this.plansFile = plansFile;
		this.outputDir = outputDir;
		this.iterations = iterations;
		this.writeEventsForAllIts = writeEventsForAllIts;
		this.useLanes = useLanes;
		this.lanesInfo = lanesInfo;
		this.capMain = capMain;
		this.capFirstLast = capFirstLast;
		this.linkTTs = linkTTs;
		this.propChangeExpBeta = propChangeExpBeta;
		this.propReRoute = propReRoute;
		this.propKeepLast = propKeepLast;
		this.propSelectRandom = propSelectRandom;
		this.propSelectExpBeta = propSelectExpBeta;
		this.propBestScore = propBestScore;
		this.brainExpBeta = brainExpBeta;
		this.travelTimeBinSize = ttBinSize;
		this.sigma = sigma;
		this.monetaryDistanceCostRate = monetaryDistanceCostRate;
		this.enableLinkToLinkRouting = enableLinkToLinkRouting;
	}

	private void prepareAndRunAndAnalyse() {
		// write some information
		log.info("Starts running the simulation from the input directory " + inputDir);
		log.info("The output will be written to " + outputDir);
		
		// prepare the simulation		
		Config config = adaptConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		adaptNetwork(scenario);

		// prepare the controller
		Controler controler = new Controler(scenario);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		// adapt sigma for randomized routing
		final RandomizingTimeDistanceTravelDisutility.Builder builder = new RandomizingTimeDistanceTravelDisutility.Builder();
		builder.setSigma(sigma);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindTravelDisutilityFactory().toInstance(builder);
			}
		});

		// run the simulation
		controler.run();

		// analyze the simulation
		String analyzeDir = outputDir + "analysis/";
		new File(analyzeDir).mkdir();
		AnalyzeBraessSimulation analyzer = new AnalyzeBraessSimulation(
				outputDir, iterations, analyzeDir);
		if (writeEventsForAllIts)
			// analyze all iterations in terms of route choice and travel time
			analyzer.analyzeAllIt();
		// analyze the last iteration more detailed
		analyzer.analyzeLastIt();
	}

	private Config adaptConfig() {
		// read config file
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, basicConfigFile);

		// adapt output directory
		config.controler().setOutputDirectory(outputDir);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		
		config.vspExperimental().setWritingOutputEvents(true);
		
		config.planCalcScore().setWriteExperiencedPlans(true);

		// adapt number of iterations
		config.controler().setLastIteration(iterations);

		if (writeEventsForAllIts) {
			// adapt events writing interval
			config.controler().setWriteEventsInterval(1);
		}

		// set network and lane properties
		config.network().setInputFile(basicNetworkFile);
		config.scenario().setUseLanes(useLanes);
		config.network().setLaneDefinitionsFile(
				inputDir + lanesInfo + capMain + "-" + capFirstLast + ".xml");

		config.planCalcScore().setBrainExpBeta(brainExpBeta);

		config.controler().setLinkToLinkRoutingEnabled(enableLinkToLinkRouting);
		
		// adapt travelTimeBinSize and travelTimeCalculatorType
		config.travelTimeCalculator().setTraveltimeBinSize(travelTimeBinSize);
		log.info("Hash Map is used as default travel time calculator.");
		config.travelTimeCalculator().setTravelTimeCalculatorType(
				TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());

		// adapt strategies
		Collection<StrategySettings> strategySettings = config.strategy()
				.getStrategySettings();
		for (StrategySettings s : strategySettings) {
			if (s.getStrategyName().equals("ReRoute")) {
				s.setWeight(propReRoute);
//				s.setDisableAfter(iterations / 2);
				s.setDisableAfter(50) ;
			}
			if (s.getStrategyName().equals("ChangeExpBeta")) {
				s.setWeight(propChangeExpBeta);
			}
		}

		StrategySettings keepLastSelectedStrategy = new StrategySettings();
		keepLastSelectedStrategy.setStrategyName("KeepLastSelected");
		keepLastSelectedStrategy.setWeight(propKeepLast);
		keepLastSelectedStrategy.setDisableAfter(iterations / 2);
		config.strategy().addStrategySettings(keepLastSelectedStrategy);

		StrategySettings selectRandomStrategy = new StrategySettings();
		selectRandomStrategy.setStrategyName( DefaultSelector.SelectRandom.toString() );
		selectRandomStrategy.setWeight(propSelectRandom);
//		selectRandomStrategy.setDisableAfter(iterations / 2);
		selectRandomStrategy.setDisableAfter(900);
		config.strategy().addStrategySettings(selectRandomStrategy);

		StrategySettings selExpBetaStrategy = new StrategySettings();
		selExpBetaStrategy.setStrategyName("SelectExpBeta");
		selExpBetaStrategy.setWeight(propSelectExpBeta);
		config.strategy().addStrategySettings(selExpBetaStrategy);

		StrategySettings bestScoreStrategy = new StrategySettings();
		bestScoreStrategy.setStrategyName("BestScore");
		bestScoreStrategy.setWeight(propBestScore);
		config.strategy().addStrategySettings(bestScoreStrategy);
		
		config.strategy().setMaxAgentPlanMemorySize(0);

		// set plans file
		config.plans().setInputFile(plansFile);

		// adapt monetary distance cost rate
		config.planCalcScore().setMonetaryDistanceCostRateCar(
				monetaryDistanceCostRate);
		return config;
	}

	private void adaptNetwork(Scenario scenario) {
		// collect all links from the network
		Link[] links = new Link[7];
		for (int id = 1; id <= 7; id++){
			links[id-1] = scenario.getNetwork().getLinks()
					.get(Id.create(id, Link.class));
		}
		
		log.info("In the basic network all links have length 200m");
	
		// set travel times at the links (by adapting free speed)
		for (int id = 1; id <= 7; id++){
			if (linkTTs[id-1] != 0.0)
				links[id-1].setFreespeed(links[id-1].getLength() / linkTTs[id-1]);
			else // specific linkTT == 0.0
				log.error("The link travel time can't be zero. Choose at least one second.");
		}
	
		// adapt capacity on all links
		links[1-1].setCapacity(capFirstLast);
		for (int id = 2; id <= 6; id++){
			links[id-1].setCapacity(capMain);
		}
		links[7-1].setCapacity(capFirstLast);
	}

	public static void main(String[] args) {
		String date = "2015-05-28";

		int numberOfAgents = 3600;
		boolean sameStartTime = true;
		boolean initPlansWithAllRoutes = true;

		int iterations = 1000;
		boolean writeEventsForAllIts = true; // needed for detailed analysis.
												// remind the running time if
												// true

		// define travel times on links
		double[] linkTTs = new double[7];
		/* tt on the middle link */
		linkTTs[4 - 1] = 1; // [s]. for deleting use 200
		/* tt of the link which is not at the middle route. */
		linkTTs[3 - 1] = 20; // [s]
		linkTTs[5 - 1] = 20; // [s]
		/* tt on the first link where the activity is located */
		linkTTs[1 - 1] = 1; // [s]
		linkTTs[7 - 1] = 1; // [s]

		boolean useLanes = false;
		String lanesInfo = "lanes";

		long capMain = 1800;
		long capFirstLast = 3600;

		double propChangeExpBeta = 0.9;
		double propReRoute = 0.1;
		double propKeepLast = 0.0;
		double propSelectRandom = 0.1;
		double propSelectExpBeta = 0.0;
		double propBestScore = 0.0;

		double brainExpBeta = 20.0; // default: 1.0. DG used to use 2.0 - better
									// results!?

		int ttBinSize = 1; // [s]

		// choose a sigma for the randomized router
		// (higher sigma cause more randomness. use 0.0 for no randomness.)
		double sigma = 0.0;
		// choose the monetary cost rate for traveled distance
		// (should be negative. use -12.0 to balance time [h] and distance [m].
		// use -0.00015 to approximately balance travel time and distance in
		// this scenario.
		// use -0.0 to use only time.)
		double monetaryDistanceCostRate = 0.0; // -0.00015;

		boolean enableLinkToLinkRouting = true;

		String inputDir = DgPaths.SHAREDSVN
				+ "studies/tthunig/scenarios/BraessWoSignals/";

		// create run name
		String info = createRunName(numberOfAgents, sameStartTime,
				initPlansWithAllRoutes, iterations, linkTTs, useLanes,
				lanesInfo, capMain, propChangeExpBeta, propReRoute,
				propKeepLast, propSelectRandom, propSelectExpBeta,
				propBestScore, brainExpBeta, ttBinSize, sigma,
				monetaryDistanceCostRate, enableLinkToLinkRouting);

		String outputDir = inputDir + "matsim-output/" + date + "_" + info
				+ "/";
		outputDir = "/Users/nagel/kairuns/braess/output" ;
		
		String basicConfig = inputDir + "basicConfig.xml";
		String basicNetwork = inputDir + "basicNetwork.xml";
		String plansFile = inputDir
				+ createPlansFileName(numberOfAgents, sameStartTime,
						initPlansWithAllRoutes);

		RunBraessWoSignals runScript = new RunBraessWoSignals(inputDir,
				basicConfig, basicNetwork, plansFile, outputDir, iterations,
				writeEventsForAllIts, linkTTs, useLanes,
				lanesInfo, capMain, capFirstLast, propChangeExpBeta,
				propReRoute, propKeepLast, propSelectRandom, propSelectExpBeta,
				propBestScore, brainExpBeta, ttBinSize, sigma,
				monetaryDistanceCostRate, enableLinkToLinkRouting);
		runScript.prepareAndRunAndAnalyse();

	}

	private static String createPlansFileName(int numberOfAgents,
			boolean sameStartTime, boolean initPlansWithAllRoutes) {

		String plansFile = "plans" + numberOfAgents;
		if (sameStartTime) {
			plansFile += "SameStartTime";
		}
		if (initPlansWithAllRoutes)
			plansFile += "AllRoutes";
		plansFile += ".xml";

		log.info("Use plans file " + plansFile);
		return plansFile;
	}

	private static String createRunName(int numberOfAgents,
			boolean sameStartTime, boolean initPlansWithAllRoutes,
			int iterations, double[] linkTTs, boolean useLanes,
			String lanesInfo, long capMain, double propChangeExpBeta,
			double propReRoute, double propKeepLast, double propSelectRandom,
			double propSelectExpBeta, double propBestScore,
			double brainExpBeta, int ttBinSize, double sigma,
			double monetaryDistanceCostRate, boolean enableLinkToLinkRouting) {

		String info = numberOfAgents + "p";
		if (sameStartTime)
			info += "_sameTime";
		info += "_" + iterations + "it";

		info += "_cap" + capMain;
		info += "_ttMid" + linkTTs[4 - 1] + "s";
		if (linkTTs[3 - 1] != 20.0 || linkTTs[5 - 1] != 20.0)
			info += "_tt3-" + linkTTs[3 - 1] + "s" + "_tt5-" + linkTTs[5 - 1]
					+ "s";
		if (linkTTs[1 - 1] != 0.0)
			info += "_tt1-" + linkTTs[1 - 1];
		if (linkTTs[7 - 1] != 0.0)
			info += "_tt7-" + linkTTs[7 - 1];

		if (propChangeExpBeta != 0.0)
			info += "_chExpBeta" + propChangeExpBeta;
		if (propReRoute != 0.0)
			info += "_reRoute" + propReRoute;
		if (propKeepLast != 0.0)
			info += "_keepLast" + propKeepLast;
		if (propSelectRandom != 0.0)
			info += "_selRandom" + propSelectRandom;
		if (propSelectExpBeta != 0.0)
			info += "_selExpBeta" + propSelectExpBeta;
		if (propBestScore != 0.0)
			info += "_bestScore" + propBestScore;
		if (propSelectExpBeta != 0.0 || propChangeExpBeta != 0.0)
			info += "_beta" + brainExpBeta;

		info += "_ttBinSize" + ttBinSize;

		if (sigma != 0.0)
			info += "_sigma" + sigma;
		if (monetaryDistanceCostRate != 0.0)
			info += "_distCost" + monetaryDistanceCostRate;

		if (useLanes)
			info += "_" + lanesInfo;

		if (enableLinkToLinkRouting)
			info += "_link2link";
		else
			info += "_node2node";
		return info;
	}

}
