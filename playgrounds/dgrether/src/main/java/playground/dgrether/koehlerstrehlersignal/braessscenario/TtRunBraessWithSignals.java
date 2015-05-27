package playground.dgrether.koehlerstrehlersignal.braessscenario;

import java.io.File;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.signals.data.SignalsData;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.analysis.AnalyzeBraessSimulation;

public class TtRunBraessWithSignals {
	
	private static final Logger log = Logger.getLogger(TtRunBraessWithSignals.class);
	
	private String inputDir;
	private String basicConfigFile;	
	private String basicNetworkFile;
	private String plansFile;
	private String signalControlFile;
	private String outputDir;
	private int iterations;
	private boolean writeEventsForAllIts; 
	private boolean useLanes;
	private long capMain;
	private long capFirstLast;
	private int ttMid;
	private double propChangeExpBeta;
	private double propReRoute;
	private double propKeepLast;
	private double propSelectRandom;
	private double propSelectExpBeta;
	private double propBestScore;
	private double brainExpBeta;
	private int travelTimeBinSize;
	private TravelTimeCalculatorType travelTimeCalculator;
	private double sigma;
	private double monetaryDistanceCostRate;
	private boolean enableLinkToLinkRouting;

	
	public TtRunBraessWithSignals(String inputDir, String basicConfigFile,
			String basicNetworkFile, String plansFile,
			String signalControlFile, String outputDir, int iterations,
			boolean writeEventsForAllIts, boolean useLanes, long capMain,
			long capFirstLast, int ttMid, double propChangeExpBeta,
			double propReRoute, double propKeepLast, double propSelectRandom,
			double propSelectExpBeta, double propBestScore,
			double brainExpBeta, int travelTimeBinSize,
			TravelTimeCalculatorType travelTimeCalculator, double sigma,
			double monetaryDistanceCostRate, boolean enableLinkToLinkRouting) {
		
		this.inputDir = inputDir;
		this.basicConfigFile = basicConfigFile;	
		this.basicNetworkFile = basicNetworkFile;
		this.plansFile = plansFile;
		this.signalControlFile = signalControlFile;
		this.outputDir = outputDir;
		this.iterations = iterations;
		this.writeEventsForAllIts = writeEventsForAllIts; 
		this.useLanes = useLanes;
		this.capMain = capMain;
		this.capFirstLast = capFirstLast;
		this.ttMid = ttMid;
		this.propChangeExpBeta = propChangeExpBeta;
		this.propReRoute = propReRoute;
		this.propKeepLast = propKeepLast;
		this.propSelectRandom = propSelectRandom;
		this.propSelectExpBeta = propSelectExpBeta;
		this.propBestScore = propBestScore;
		this.brainExpBeta = brainExpBeta;	
		this.travelTimeBinSize = travelTimeBinSize;
		this.travelTimeCalculator = travelTimeCalculator;
		this.sigma = sigma;
		this.monetaryDistanceCostRate = monetaryDistanceCostRate;
		this.enableLinkToLinkRouting = enableLinkToLinkRouting;
	}

	/**
	 * prepares the run in terms of config properties and network capacity 
	 * (with the properties given in the constructor)
	 * 
	 * runs the simulation
	 * 
	 * and analyzes it afterwards in terms of route choice and travel time
	 */
	public void prepareAndRunAndAnalyze() {
		// write some information
		log.info("Starts running the simulation from the input directory " + inputDir);
		log.info("The output will be written to " + outputDir);
		
		// prepare the simulation
		Config config = adaptConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, 
				new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		adaptNetwork(scenario);
		
		// prepare the controller
		Controler controler = new Controler(scenario);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.addOverridingModule(new SignalsModule());
		// adapt sigma for randomized routing
		final RandomizingTimeDistanceTravelDisutility.Builder builder =
				new RandomizingTimeDistanceTravelDisutility.Builder();
		builder.setSigma(this.sigma);
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
		AnalyzeBraessSimulation analyzer = new AnalyzeBraessSimulation(outputDir, iterations, analyzeDir);
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

		// adapt number of iterations
		config.controler().setLastIteration(iterations);

		if (writeEventsForAllIts) {
			// adapt events writing interval
			config.controler().setWriteEventsInterval(1);
		}
		
		// set network lanes and signal files
		config.network().setInputFile(basicNetworkFile);
		config.scenario().setUseLanes(useLanes);
		config.network().setLaneDefinitionsFile(inputDir + "lanes" + capMain + "-" + capFirstLast + ".xml");
		config.signalSystems().setSignalControlFile(signalControlFile);
		log.info("Used signal groups file: " + config.signalSystems().getSignalGroupsFile());
		if (!useLanes)
			config.signalSystems().setSignalSystemFile(inputDir + "signalSystemsWoLanes.xml");
		else
			config.signalSystems().setSignalSystemFile(inputDir + "signalSystems.xml");
		log.info("Used signal systems file: " + config.signalSystems().getSignalSystemFile());		
		
		// adapt plans file
		config.plans().setInputFile(plansFile);
		Log.info("Use plans file " + plansFile);
		
		// adapt strategies
		config.planCalcScore().setBrainExpBeta(brainExpBeta);

		Collection<StrategySettings> strategySettings = config.strategy()
				.getStrategySettings();
		for (StrategySettings s : strategySettings) {
			if (s.getStrategyName().equals("ReRoute")) {
				s.setWeight(propReRoute);
				s.setDisableAfter(iterations / 2);
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
		selectRandomStrategy.setStrategyName("SelectRandom");
		selectRandomStrategy.setWeight(propSelectRandom);
		selectRandomStrategy.setDisableAfter(iterations / 2);
		config.strategy().addStrategySettings(selectRandomStrategy);

		StrategySettings selExpBetaStrategy = new StrategySettings();
		selExpBetaStrategy.setStrategyName("SelectExpBeta");
		selExpBetaStrategy.setWeight(propSelectExpBeta);
		config.strategy().addStrategySettings(selExpBetaStrategy);

		StrategySettings bestScoreStrategy = new StrategySettings();
		bestScoreStrategy.setStrategyName("BestScore");
		bestScoreStrategy.setWeight(propBestScore);
		config.strategy().addStrategySettings(bestScoreStrategy);
		
		// adapt travelTimeBinSize and travelTimeCalculatorType
		config.travelTimeCalculator().setTraveltimeBinSize(travelTimeBinSize);
		config.travelTimeCalculator().setTravelTimeCalculatorType(travelTimeCalculator.toString());

		// // adapt utils
		// config.planCalcScore().setTraveling_utils_hr(travelingUtils);
		// config.planCalcScore().setPerforming_utils_hr(performingUtils);
		// config.planCalcScore().setLateArrival_utils_hr(lateArrivalUtils);
		// config.planCalcScore().getActivityParams("dummy").setScoringThisActivityAtAll(scoringDummy);
		config.planCalcScore().setMonetaryDistanceCostRateCar(monetaryDistanceCostRate);
		
		config.controler().setLinkToLinkRoutingEnabled(enableLinkToLinkRouting);
		
		return config;
	}

	private void adaptNetwork(Scenario scenario) {
		// adapt capacity
		for (Link l : scenario.getNetwork().getLinks().values()){
			if (l.getId().equals(Id.create(1, Link.class)) || l.getId().equals(Id.create(7, Link.class)))
				l.setCapacity(capFirstLast);
			else
				l.setCapacity(capMain);
		}
		
		// adapt travel time on the middle link by adapting it's freespeed
		Link middleLink = scenario.getNetwork().getLinks().get(Id.create(4, Link.class));
		middleLink.setFreespeed(middleLink.getLength() / ttMid);
		
		log.info("The link travel times are " + 
				"1s on the first link, " + 
				"10s-" + ttMid + "s-10s on the middle path, " + 
				"10s-20s or 20s-10s respectively on the other paths " + 
				"and 0s on the last link.");
	}

	public static void main(String[] args) {
		String date = "2015-05-27";

		int numberOfAgents = 60;
		boolean sameStartTime = true;
		boolean initPlansWithAllRoutes = true;

		int iterations = 100;
		boolean writeEventsForAllIts = true; // for detailed analysis.
											//	remind the running time if true

		boolean useLanes = true;
		
		long capMain = 1800;
		long capFirstLast = 3600;
		
		int ttMid = 1; // [s]. for deleting use 200

		// double performingUtils = 6.0; // +6 default
		// double travelingUtils = -6.0; // -6 default
		// double lateArrivalUtils = -18.0; // default -18
		// boolean scoringDummy = true; // default true

		double propChangeExpBeta = 0.9;
		double propReRoute = 0.1;
		double propKeepLast = 0.0;
		double propSelectRandom = 0.0;
		double propSelectExpBeta = 0.0;
		double propBestScore = 0.0;

		double brainExpBeta = 2.0; // default: 1.0. DG used to use 2.0

		// boolean enableLinkToLinkRouting = true; // default is true
		int travelTimeBinSize = 3;
		TravelTimeCalculatorType travelTimeCalculator = TravelTimeCalculatorType.TravelTimeCalculatorHashMap;

		// choose a sigma for the randomized router
		// (higher sigma cause more randomness. use 0.0 for no randomness.)
		double sigma = 0.0;
		// (should be negative. use -12.0 to balance time [h] and distance [m]. 
		// use -0.00015 to approximately balance travel time and distance in this scenario. 
		// use -0.0 to use only time.)
		double monetaryDistanceCostRate = 0.0; //-0.00015; 
		
		boolean enableLinkToLinkRouting = true;

		String inputDir = DgPaths.SHAREDSVN
				+ "projects/cottbus/data/scenarios/braess_scenario/";
		
		// choose the signal plan: _Green or _NoZ or _BC or _All1s ...
		String signalControlFile = inputDir + "signalControl_NoZ.xml";

		String basicConfig = inputDir + "basicConfig.xml";
		String basicNetwork = inputDir + "basicNetwork.xml";
		String plansFile = inputDir
				+ createPlansFileName(numberOfAgents,	sameStartTime, initPlansWithAllRoutes);
						
		// create run name
		String runName = createRunName(numberOfAgents, sameStartTime,
				initPlansWithAllRoutes, iterations, useLanes, capMain, ttMid,
				propChangeExpBeta, propReRoute, propKeepLast, propSelectRandom,
				propSelectExpBeta, propBestScore, propSelectExpBeta,
				brainExpBeta, travelTimeBinSize, travelTimeCalculator, sigma,
				monetaryDistanceCostRate, enableLinkToLinkRouting,
				signalControlFile);

		String outputDir = DgPaths.RUNSSVN + "cottbus/braess/" + date + "_"
				+ runName + "/";

		TtRunBraessWithSignals runScript = new TtRunBraessWithSignals(inputDir,
				basicConfig, basicNetwork, plansFile, signalControlFile,
				outputDir, iterations, writeEventsForAllIts, useLanes, 
				capMain, capFirstLast, ttMid,
				propChangeExpBeta, propReRoute, propKeepLast, propSelectRandom,
				propSelectExpBeta, propBestScore, brainExpBeta,
				travelTimeBinSize, travelTimeCalculator, sigma, 
				monetaryDistanceCostRate, enableLinkToLinkRouting);
		runScript.prepareAndRunAndAnalyze();
	}

	private static String createRunName(int numberOfAgents,
			boolean sameStartTime, boolean initPlansWithAllRoutes,
			int iterations, boolean useLanes, long capMain,
			int ttMid, double propChangeExpBeta, double propReRoute,
			double propKeepLast, double propSelectRandom,
			double propSelectExpBeta, double propBestScore,
			double propSelectExpBeta2, double brainExpBeta,
			int travelTimeBinSize,
			TravelTimeCalculatorType travelTimeCalculator, double sigma,
			double monetaryDistanceCostRate, boolean enableLinkToLinkRouting,
			String signalControlFile) {
		
		String info = numberOfAgents + "p";
		if (sameStartTime)
			info += "_sameTime";
		if (!initPlansWithAllRoutes)
			info += "_initWoRoutes";
		
		info += "_" + iterations + "it";
		
		if (!useLanes)
			info += "_woLanes";
		
		info += "_cap" + capMain;
		
		info += "_ttMid" + ttMid + "s";
		
//		if (performingUtils != 6.0)
//			info += "_perfUtil" + performingUtils;
//		if (travelingUtils != -6.0)
//			info += "_travUtil" + travelingUtils;
//		if (lateArrivalUtils != -18.0)
//			info += "_lateArr" + lateArrivalUtils;
//		if (!scoringDummy)
//			info += "_dontScoreDummyAct";
		
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
		
		info += "_ttBinSize" + travelTimeBinSize;
//		if (travelTimeCalculator.equals(TravelTimeCalculatorType.TravelTimeCalculatorArray))
//			info += "_ttCalcArray";
//		else if (travelTimeCalculator.equals(TravelTimeCalculatorType.TravelTimeCalculatorHashMap))
//			info += "_ttCalcMap";
		
		if (sigma != 0.0)
			info += "_randRouter" + sigma;
		if (monetaryDistanceCostRate != 0.0)
			info += "_distCost" + monetaryDistanceCostRate;
		
		if (enableLinkToLinkRouting)
			info += "_link2link";
		else
			info += "_node2node";
		
		// collect signal control information (the term between _ and . in the end)
		String[] array1 = signalControlFile.split("_");
		String[] array2 = (array1[array1.length-1]).split("\\.");
		info += "_signals" + array2[0];
		
		return info;
	}

	private static String createPlansFileName(int numberOfAgents, 
			boolean sameStartTime, boolean initPlansWithAllRoutes) {
		
		String plansFile = "plans" + numberOfAgents;
		if (sameStartTime){
			plansFile += "sameStartTime";
		}
		if (initPlansWithAllRoutes)
			plansFile += "AllRoutes";
		plansFile += ".xml";
		
		return plansFile;
	}
	
	
}
