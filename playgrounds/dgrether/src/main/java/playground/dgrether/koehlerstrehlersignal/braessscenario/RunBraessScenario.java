package playground.dgrether.koehlerstrehlersignal.braessscenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.signals.data.SignalsData;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;

/**
 * Class to run the MATSim simulation of breaess's scenario
 * 
 * @author tthunig
 * 
 */
public class RunBraessScenario {
	
	private String networkFile;
	private String lanesFile;
	private String signalsSystemsFile;
	private String signalGroupsFile;
	private String signalsControlFile;
	private String plansFile;
	
	private int firstIteration;
	private int lastIteration;
	private int writeInterval;
	private int maxPlans;
	private double propChangeExpBeta;
	private double propReRoute;
	private int reRouteDisableAfter;
	private boolean enableLinkToLinkRouting;
	private int timeBinSize;
	private TravelTimeCalculatorType travelTimeCalculator;
	
	private String outputDirectory;

	public RunBraessScenario(String networkFile,
			String lanesFile, String signalSystemsFile,
			String signalGroupsFile, String signalControlFile,
			String plansFile, int firstIteration, int lastIteration,
			int writeInterval, int maxPlans, double propChangeExpBeta,
			double propReRoute, int reRouteDisableAfter, 
			boolean enableLinkToLinkRouting, int timeBinSize, 
			TravelTimeCalculatorType travelTimeCalculator, 
			String outputDirectory) {
		
		this.networkFile = networkFile;
		this.lanesFile = lanesFile;
		this.signalsSystemsFile = signalSystemsFile;
		this.signalGroupsFile = signalGroupsFile;
		this.signalsControlFile = signalControlFile;
		this.plansFile = plansFile;
		
		this.firstIteration = firstIteration;
		this.lastIteration = lastIteration;
		this.writeInterval = writeInterval;
		this.maxPlans = maxPlans;
		this.propChangeExpBeta = propChangeExpBeta;
		this.propReRoute = propReRoute;
		this.reRouteDisableAfter = reRouteDisableAfter;
		this.enableLinkToLinkRouting = enableLinkToLinkRouting;
		this.timeBinSize = timeBinSize;
		this.travelTimeCalculator = travelTimeCalculator;	
		
		this.outputDirectory = outputDirectory;
	}

	private void run() {
		Config config = ConfigUtils.createConfig();

		// set config parameter
		config.network().setInputFile(this.networkFile);
		config.network().setLaneDefinitionsFile(this.lanesFile);
		
		config.plans().setInputFile(this.plansFile);
		
		config.scenario().setUseLanes(true);
		config.scenario().setUseSignalSystems(true);
		
		config.controler().setLinkToLinkRoutingEnabled(this.enableLinkToLinkRouting);
		config.controler().setOutputDirectory(this.outputDirectory);
		config.controler().setFirstIteration(this.firstIteration);
		config.controler().setLastIteration(this.lastIteration);
		config.controler().setWriteEventsInterval(this.writeInterval);
		config.controler().setWritePlansInterval(this.writeInterval);
		config.controler().setWriteSnapshotsInterval(this.writeInterval);
		
		config.signalSystems().setSignalControlFile(this.signalsControlFile);
		config.signalSystems().setSignalGroupsFile(this.signalGroupsFile);
		config.signalSystems().setSignalSystemFile(this.signalsSystemsFile);
		
		config.qsim().setStartTime(0*3600);
		config.qsim().setEndTime(0*3600);
		config.qsim().setSnapshotPeriod(1*60);
		
		config.planCalcScore().setLearningRate(1.0); //1.0 default
		config.planCalcScore().setBrainExpBeta(2.0); //1.0 default TODO ?
		config.planCalcScore().setLateArrival_utils_hr(-18.0); //-18.0 default
		config.planCalcScore().setEarlyDeparture_utils_hr(0.0); //0.0 default
		config.planCalcScore().setPerforming_utils_hr(6.0); //6.0 default
		config.planCalcScore().setTraveling_utils_hr(-6.0); //-6.0 default
		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0.0); //0.0 default
		
		// TODO try to use random router
//		TravelTime timeCalculator = config.travelTimeCalculator().get;
//		PlanCalcScoreConfigGroup cnScoringGroup = config.planCalcScore();
//		RandomizingTimeDistanceTravelDisutility rTDTD = (RandomizingTimeDistanceTravelDisutility)
//				(new RandomizingTimeDistanceTravelDisutility.Builder()).createTravelDisutility(
//						timeCalculator, cnScoringGroup);
//		config.planCalcScore().		
				
		
		ActivityParams dummyParam = new ActivityParams("dummy");
		dummyParam.setTypicalDuration(12*3600);
		dummyParam.setLatestStartTime(11*3600);
		dummyParam.setOpeningTime(7*3600);
		config.planCalcScore().addActivityParams(dummyParam);
		
		config.strategy().setMaxAgentPlanMemorySize(this.maxPlans);
		StrategySettings changeExpBetaStrategy = new StrategySettings();
		changeExpBetaStrategy.setStrategyName("ChangeExpBeta");
		changeExpBetaStrategy.setWeight(this.propChangeExpBeta);
		config.strategy().addStrategySettings(changeExpBetaStrategy);
		StrategySettings reRouteStrategy = new StrategySettings();
		reRouteStrategy.setStrategyName("ReRoute");
		reRouteStrategy.setWeight(this.propReRoute);
		reRouteStrategy.setDisableAfter(this.reRouteDisableAfter);
		config.strategy().addStrategySettings(reRouteStrategy);
		
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		config.travelTimeCalculator().setTraveltimeBinSize(this.timeBinSize);
		config.travelTimeCalculator().setTravelTimeCalculatorType(
				this.travelTimeCalculator.toString());
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		
		// create controler
		Controler controler = new Controler(scenario);
		controler
				.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.addOverridingModule(new SignalsModule());
		
		DgSylviaConfig sylviaConfig = new DgSylviaConfig();
		final DgSylviaControlerListenerFactory signalsFactory = 
				new DgSylviaControlerListenerFactory(sylviaConfig);
		// note: This will check (in DefaultSignalModelFactory) if the
		// controllerIdentifier equals sylvia..., otherwise the default
		// (fixed time) signal controller will be used. kai & theresa, oct'14

//		signalsFactory.setAlwaysSameMobsimSeed(true);
		controler.addControlerListener(signalsFactory
				.createSignalsControllerListener());

		controler.setOverwriteFiles(true);
		controler.run();
	}

	/**
	 * starts the simulation
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {

		String date = "2015-04-14";
		int simulationCase = 1; // 1 - base case, 2 - base case continued, 3 - changed signals
		
		// BASE CASE - Case 1
		
		String inputDir = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/";
		// attention: lanes depend on capacity and link length
		String lanesFile = inputDir + "laneDefinitions_8640_firstLast5s.xml"; 
		String signalSystemsFile = inputDir + "signalSystems_v2.0.xml";
		String signalGroupsFile = inputDir + "signalGroups_v2.0.xml";
//		String signalControlFile = inputDir + "signalControl_green.xml";
		String signalControlFile = inputDir + "signalControl_BC.xml";
		String plansFile = inputDir + "plans60.xml";
		
		String cap = "8640"; // link capacity in the network
		
		List<String> ttZs = new ArrayList<>(); // travel time on the middle link
//		ttZs.add("0s");
		ttZs.add("5s");
//		ttZs.add("10s");
//		ttZs.add("200s");
		
		for (String ttZ : ttZs){
			
			String networkFile = inputDir + "network_" + cap + "_" + ttZ
					+ "_firstLast5s.xml";

			int firstIteration = 0;
			int lastIteration = 100;
			int writeInterval = 100;
			int maxPlans = 5;
			double propChangeExpBeta = 0.9;
			double propReRoute = 0.1;
			int reRouteDisableAfter = 50;
			boolean enableLinkToLinkRouting = true;
			int timeBinSize = 1;
			TravelTimeCalculatorType travelTimeCalculator = TravelTimeCalculatorType.TravelTimeCalculatorArray;

			String outputDir = DgPaths.REPOS + "runs-svn/cottbus/braess/"
					+ date + "_tbs" + timeBinSize + "_net" + cap + "-" + ttZ
					+ "_basecase";

			RunBraessScenario controler = null;
			if (simulationCase == 1) {
				controler = new RunBraessScenario(networkFile, lanesFile,
						signalSystemsFile, signalGroupsFile, signalControlFile,
						plansFile, firstIteration, lastIteration,
						writeInterval, maxPlans, propChangeExpBeta,
						propReRoute, reRouteDisableAfter,
						enableLinkToLinkRouting, timeBinSize,
						travelTimeCalculator, outputDir);
				controler.run();
			}

			// BASE CASE CONTINUED - Case 2

			int firstIterationContinued = lastIteration;
			int lastIterationContinued = 200;
			int reRouteDisableAfterContinued = 150;
			outputDir = DgPaths.REPOS + "runs-svn/cottbus/braess/" + date
					+ "_tbs" + timeBinSize + "_net" + cap + "-" + ttZ
					+ "_basecaseContinued";

			if (simulationCase == 2) {
				controler = new RunBraessScenario(networkFile, lanesFile,
						signalSystemsFile, signalGroupsFile, signalControlFile,
						plansFile, firstIterationContinued,
						lastIterationContinued, writeInterval, maxPlans,
						propChangeExpBeta, propReRoute,
						reRouteDisableAfterContinued, enableLinkToLinkRouting,
						timeBinSize, travelTimeCalculator, outputDir);
				controler.run();
			}

			// CHANGED SIGNALS - Case 3

			String btuDir = DgPaths.REPOS
					+ "shared-svn/projects/cottbus/data/"
					+ "optimization/braess2ks/2015-02-24_minflow_1.0_morning_peak_"
					+ "speedFilter1.0_SP_tt_cBB100.0_sBB500.0/btu/";
			List<String> coordNames = new ArrayList<>();
			coordNames.add("minCoord");
			coordNames.add("greenWaveZ");
			coordNames.add("maxCoord");
//			coordNames.add("maxCoordEmptyZ");
//			coordNames.add("maxCoordFullZ");
//			coordNames.add("minCoordFullZ");

			if (simulationCase == 3) {
				for (String coordName : coordNames) {
					String changedSignalSystemsFile = btuDir
							+ "signal_systems_" + coordName
							+ "_fix_coordinations.xml";
					String changedSignalGroupsFile = btuDir + "signal_groups_"
							+ coordName + "_fix_coordinations.xml";
					String changedSignalControlFile = btuDir
							+ "signal_control_" + coordName
							+ "_fix_coordinations.xml";
					outputDir = DgPaths.REPOS + "runs-svn/cottbus/braess/"
							+ date + "_tbs" + timeBinSize + "_net" + cap + "-"
							+ ttZ + "_" + coordName;

					controler = new RunBraessScenario(networkFile, lanesFile,
							changedSignalSystemsFile, changedSignalGroupsFile,
							changedSignalControlFile, plansFile,
							firstIterationContinued, lastIterationContinued,
							writeInterval, maxPlans, propChangeExpBeta,
							propReRoute, reRouteDisableAfterContinued,
							enableLinkToLinkRouting, timeBinSize,
							travelTimeCalculator, outputDir);
					controler.run();
				}
			}
		}		
	}

}
