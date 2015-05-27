package playground.dgrether.koehlerstrehlersignal.braessscenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.signals.data.SignalsData;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;

/**
 * Class to run a MATSim simulation of breaess's scenario
 * 
 * @author tthunig
 * @deprecated use TtRunBraessWithSignals instead or actualize this class for using btu signals
 */
public class RunBraessScenario {
	
	private String networkFile;
	private String lanesFile;
	private String signalsSystemsFile;
	private String signalGroupsFile;
	private String signalsControlFile;
	private String plansFile;

	private boolean enforceZ;
	private boolean forbitZ;
	
	private int firstIteration;
	private int lastIteration;
	private int writeInterval;
	private int maxPlans;
	private double travelingUtils;
	private double propChangeExpBeta;
	private double brainExpBeta;
	private double propReRoute;
	private int reRouteDisableAfter;
	private boolean enableLinkToLinkRouting;
	private int timeBinSize;
	private TravelTimeCalculatorType travelTimeCalculator;
	private boolean useRandomizedRouter;
	private double sigma;
	
	private String outputDirectory;

	public RunBraessScenario(String networkFile,
			String lanesFile, String signalSystemsFile,
			String signalGroupsFile, String signalControlFile,
			String plansFile, boolean enforceZ, boolean forbitZ, 
			int firstIteration, int lastIteration,
			int writeInterval, int maxPlans, double travelingUtils,
			double propChangeExpBeta, double propReRoute, 
			double brainExpBeta, int reRouteDisableAfter, 
			boolean enableLinkToLinkRouting, int timeBinSize, 
			TravelTimeCalculatorType travelTimeCalculator, 
			boolean useRandomizedRouter, double sigma,
			String outputDirectory) {
		
		this.networkFile = networkFile;
		this.lanesFile = lanesFile;
		this.signalsSystemsFile = signalSystemsFile;
		this.signalGroupsFile = signalGroupsFile;
		this.signalsControlFile = signalControlFile;
		this.plansFile = plansFile;

		this.enforceZ = enforceZ;
		this.forbitZ = forbitZ;
		
		this.firstIteration = firstIteration;
		this.lastIteration = lastIteration;
		this.writeInterval = writeInterval;
		this.maxPlans = maxPlans;
		this.travelingUtils = travelingUtils;
		this.propChangeExpBeta = propChangeExpBeta;
		this.brainExpBeta = brainExpBeta;
		this.propReRoute = propReRoute;
		this.reRouteDisableAfter = reRouteDisableAfter;
		this.enableLinkToLinkRouting = enableLinkToLinkRouting;
		this.timeBinSize = timeBinSize;
		this.travelTimeCalculator = travelTimeCalculator;
		this.useRandomizedRouter = useRandomizedRouter;
		this.sigma = sigma;
		
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
		config.planCalcScore().setBrainExpBeta(this.brainExpBeta); //1.0 default
		config.planCalcScore().setLateArrival_utils_hr(-18.0); //-18.0 default
		config.planCalcScore().setEarlyDeparture_utils_hr(0.0); //0.0 default
		config.planCalcScore().setPerforming_utils_hr(6.0); //6.0 default
		config.planCalcScore().setTraveling_utils_hr(this.travelingUtils); //-6.0 default
		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0.0); //0.0 default		
		
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
		
		if (this.enforceZ){
			scenario.getNetwork().getLinks().get(Id.create(3, Link.class)).setFreespeed(1);
			scenario.getNetwork().getLinks().get(Id.create(5, Link.class)).setFreespeed(1);
		}
		if (this.forbitZ){
			scenario.getNetwork().getLinks().get(Id.create(4, Link.class)).setFreespeed(1);
		}
		
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		
		// create controler
		Controler controler = new Controler(scenario);
		controler
				.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.addOverridingModule(new SignalsModule());
		
		DgSylviaConfig sylviaConfig = new DgSylviaConfig();
		final DgSylviaControlerListenerFactory signalsFactory = 
				new DgSylviaControlerListenerFactory(sylviaConfig);
		// this will check (in DefaultSignalModelFactory) if the
		// controllerIdentifier equals sylvia, otherwise the default
		// (fixed time) signal controller will be used

//		signalsFactory.setAlwaysSameMobsimSeed(true);
		controler.addControlerListener(signalsFactory
				.createSignalsControllerListener());
		
		if (this.useRandomizedRouter){
			final RandomizingTimeDistanceTravelDisutility.Builder builder =
					new RandomizingTimeDistanceTravelDisutility.Builder();
			builder.setSigma(this.sigma);

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindTravelDisutilityFactory().toInstance(builder);
				}
			});
		}
		
//		controler.setOverwriteFiles(true);
		controler.run();
	}

	/**
	 * starts the simulation
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {

		String date = "2015-05-05";
		int simulationCase = 3; // 1 - base case, 2 - base case continued, 3 - changed signals
		
		// BASE CASE - Case 1
		
		String inputDir = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/";
		// attention: lanes depend on capacity and link length
		String lanesFile = inputDir + "laneDefinitions_8640_firstLast5s.xml"; // TODO
		String signalSystemsFile = inputDir + "signalSystems_v2.0.xml";
		String signalGroupsFile = inputDir + "signalGroups_v2.0.xml";
//		String signalControlFile = inputDir + "signalControl_green.xml";
		String signalControlFile = inputDir + "signalControl_BC.xml";
		
		String cap = "8640"; // link capacity in the network
		
		int numberOfAgents = 3600;
		String plansFile = inputDir + "plans" + numberOfAgents + ".xml";
		
		List<String> ttZs = new ArrayList<>(); // travel time on the middle link
//		ttZs.add("0s");
		ttZs.add("5s");
//		ttZs.add("10s");
//		ttZs.add("200s");
		
		boolean enforceZ = false;
		boolean forbitZ = false;
		// allowed combinations: both false, one true + one false
		
		int firstIteration = 0;
		int lastIteration = 100;
		int writeInterval = 100;
		int maxPlans = 5;
		double travelingUtils = -6.0;
		double brainExpBeta = 2.0; // 1.0 is default
		double propChangeExpBeta = 0.9;
		double propReRoute = Math.round((1.0 - propChangeExpBeta)*10) / 10.0;
		int reRouteDisableAfter = 50;
		boolean enableLinkToLinkRouting = true;
		int timeBinSize = 1;
		TravelTimeCalculatorType travelTimeCalculator = TravelTimeCalculatorType.TravelTimeCalculatorArray;
		boolean useRandomizedRouter = false; // TODO check. adapt parameter monetaryDistanceCostRateCar and others
		double sigma = 3; // TODO modify
		
		for (String ttZ : ttZs){
			
			String networkFile = inputDir + "network_" + cap + "_" + ttZ
					+ "_first1sLast5s.xml"; //TODO attention

			String outputDir = DgPaths.REPOS + "runs-svn/cottbus/braess/"
					+ date + "_tbs" + timeBinSize + "_net" + cap + "-" + ttZ + "_p" + numberOfAgents
					+ "_expBeta" + brainExpBeta + "_reRoute" + propReRoute + "_maxPlan" + maxPlans 
					+ "_trUtils" + travelingUtils;
			
			if (travelTimeCalculator.equals(TravelTimeCalculatorType.TravelTimeCalculatorArray))
				outputDir += "_ttCalcArray";
			else
				outputDir += "_ttCalcHashMap";
			if (useRandomizedRouter)
				outputDir += "_randomRouter-sigma" + sigma;
			if (enforceZ)
				outputDir += "_enforceZ";
			if (forbitZ)
				outputDir += "_forbitZ";
			

			RunBraessScenario controler = null;
			if (simulationCase == 1) {
				outputDir += "_basecase";
				controler = new RunBraessScenario(networkFile, lanesFile,
						signalSystemsFile, signalGroupsFile, signalControlFile,
						plansFile, enforceZ, forbitZ, firstIteration, lastIteration,
						writeInterval, maxPlans, travelingUtils, 
						propChangeExpBeta, brainExpBeta,
						propReRoute, reRouteDisableAfter,
						enableLinkToLinkRouting, timeBinSize,
						travelTimeCalculator, useRandomizedRouter, sigma, outputDir);
				controler.run();
			}

			// BASE CASE CONTINUED - Case 2

			int firstIterationContinued = lastIteration;
			int lastIterationContinued = 200; // TODO
			int reRouteDisableAfterContinued = lastIteration + ((lastIterationContinued-firstIterationContinued) / 2);

			if (simulationCase == 2) {
				outputDir += "_basecaseContinued";
				controler = new RunBraessScenario(networkFile, lanesFile,
						signalSystemsFile, signalGroupsFile, signalControlFile,
						plansFile, enforceZ, forbitZ, firstIterationContinued,
						lastIterationContinued, writeInterval, maxPlans,
						travelingUtils,
						propChangeExpBeta, brainExpBeta, propReRoute,
						reRouteDisableAfterContinued, enableLinkToLinkRouting,
						timeBinSize, travelTimeCalculator, useRandomizedRouter, 
						sigma, outputDir);
				controler.run();
			}

			// CHANGED SIGNALS - Case 3

			String btuDir = DgPaths.REPOS
					+ "shared-svn/projects/cottbus/data/"
					+ "optimization/braess2ks/2015-02-24_minflow_1.0_morning_peak_"
					+ "speedFilter1.0_SP_tt_cBB100.0_sBB500.0/btu/";
			List<String> coordNames = new ArrayList<>();
//			coordNames.add("minCoord");
			coordNames.add("greenWaveZ");
//			coordNames.add("maxCoord");
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

					controler = new RunBraessScenario(networkFile, lanesFile,
							changedSignalSystemsFile, changedSignalGroupsFile,
							changedSignalControlFile, plansFile, enforceZ, forbitZ, 
							firstIterationContinued, lastIterationContinued,
							writeInterval, maxPlans, travelingUtils, 
							propChangeExpBeta, brainExpBeta,
							propReRoute, reRouteDisableAfterContinued,
							enableLinkToLinkRouting, timeBinSize,
							travelTimeCalculator, useRandomizedRouter, sigma, 
							outputDir + "_" + coordName + "_it" + lastIterationContinued);
					controler.run();
				}
			}
		}		
	}

}
