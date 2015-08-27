package scenarios.braess.run;

import java.util.Calendar;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.router.InvertedNetworkTripRouterFactoryModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;
import scenarios.analysis.TtControlerListener;
import scenarios.braess.analysis.TtAnalyzeBraess;
import scenarios.braess.createInput.TtCreateBraessNetworkAndLanes;
import scenarios.braess.createInput.TtCreateBraessNetworkAndLanes.LaneType;
import scenarios.braess.createInput.TtCreateBraessPopulation;
import scenarios.braess.createInput.TtCreateBraessSignals;
import scenarios.braess.createInput.TtCreateBraessSignals.SignalControlType;

/**
 * Class to run a simulation of the braess scenario without signals. It also
 * analyzes the simulation with help of AnalyseBraessSimulation.java.
 * 
 * @author tthunig
 * 
 */
public class RunBraessSimulation {

	private static final Logger log = Logger
			.getLogger(RunBraessSimulation.class);

	/* population parameter */
	// If false, agents are initialized without any routes. If true, with all
	// three possible routes.
	private final boolean INIT_WITH_ALL_ROUTES = true;
	// initial score for all initial plans
	private final Double INIT_PLAN_SCORE = 110.;

	/// defines which kind of signals should be used: ALL_GREEN, ONE_SECOND_Z, GREEN_WAVE_Z, GREEN_WAVE_SO
	private final SignalControlType SIGNAL_TYPE = SignalControlType.GREEN_WAVE_SO;
	
	// defines which kind of lanes should be used: NONE, TRIVIAL or REALISTIC
	private static final LaneType LANE_TYPE = LaneType.REALISTIC;

	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private final double SIGMA = 0.0;	
		
	private static final boolean WRITE_INITIAL_FILES = true;
	private static String INIT_FILE_DIR = DgPaths.SHAREDSVN + "projects/cottbus/data/scenarios/braess_scenario/testRun/";

	/**
	 * prepare, run and analyze the Braess simulation
	 */
	private void prepareRunAndAnalyze() {
		log.info("Starts running the simulation.");
		
		// prepare config and scenario		
		Config config = defineConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		createNetwork(scenario);
		createPopulation(scenario);
		createRunNameAndOutputDir(scenario);

		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME,
					new SignalsScenarioLoader(signalsConfigGroup).loadSignalsData());
			createSignals(scenario);
		}
		
		// prepare the controller
		Controler controler = new Controler(scenario);

		// add the signals module if signal systems are used
		if (signalsConfigGroup.isUseSignalSystems()) {
			controler.addOverridingModule(new SignalsModule());
		}
		
		// add the module for link to link routing if enabled
		if (config.controler().isLinkToLinkRoutingEnabled()){
			controler.addOverridingModule(new InvertedNetworkTripRouterFactoryModule());
		}
		
		// adapt sigma for randomized routing
		final RandomizingTimeDistanceTravelDisutility.Builder builder = 
				new RandomizingTimeDistanceTravelDisutility.Builder();
		builder.setSigma(SIGMA);
		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				bindTravelDisutilityFactory().toInstance(builder);
			}
		});
		
		// add a controller listener to analyze results
		controler.addControlerListener(new TtControlerListener(scenario, new TtAnalyzeBraess()));
		
		// run the simulation
		controler.run();
	}

	private static Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		// set number of iterations
		config.controler().setLastIteration( 100 );

		// able or enable signals and lanes
		config.qsim().setUseLanes( LANE_TYPE.equals(LaneType.NONE)? false : true );
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils
				.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME,
						SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems( true );

		// set brain exp beta
		config.planCalcScore().setBrainExpBeta( 20 );

		// choose between link to link and node to node routing
		config.controler().setLinkToLinkRoutingEnabled( true );
		
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes( true );
		config.travelTimeCalculator().setCalculateLinkTravelTimes(true);
		
		// set travelTimeBinSize
		config.travelTimeCalculator().setTraveltimeBinSize( 10 );
		
		config.travelTimeCalculator().setTravelTimeCalculatorType(
				TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may'15
		
		// define strategies:
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultStrategy.ReRoute.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() - 50 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.SelectRandom.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() - 50 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.ChangeExpBeta.toString() );
			strat.setWeight( 1.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.BestScore.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() - 50 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.KeepLastSelected.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}

		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize( 0 );
		
		config.qsim().setStuckTime(3600 * 10.);
		
		// set end time to 12 am (4 hours after simulation start) to
		// shorten simulation run time
		config.qsim().setEndTime(3600 * 12);
		
		// adapt monetary distance cost rate
		// (should be negative. use -12.0 to balance time [h] and distance [m].
		// use -0.000015 to approximately balance the utility of travel time and
		// distance in a scenario with 3 vs 11min travel time and 40 vs 50 km.
		// use -0.0 to use only time.)
		config.planCalcScore().setMonetaryDistanceRateCar( -0.0 );

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );		
		// note: the output directory is defined in createRunNameAndOutputDir(...) after all adaptations are done
		
		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(true);

		config.controler().setWriteEventsInterval( config.controler().getLastIteration() );
		config.controler().setWritePlansInterval( config.controler().getLastIteration() );
		
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);
		
		// TODO enable this if you need the conig file
//		if (WRITE_INITIAL_FILES){
//			new ConfigWriter(config).write(INIT_FILE_DIR + "config.xml");
//		}
		
		return config;
	}

	private static void createNetwork(Scenario scenario) {	
		
		TtCreateBraessNetworkAndLanes netCreator = new TtCreateBraessNetworkAndLanes(scenario);
		netCreator.setUseBTUProperties( true );
		netCreator.setSimulateInflowCap( true );
		netCreator.setMiddleLinkExists( true );
		netCreator.setLaneType( LANE_TYPE );
		netCreator.createNetworkAndLanes();
		
		if (WRITE_INITIAL_FILES) netCreator.writeNetworkAndLanes(INIT_FILE_DIR);
	}

	private void createPopulation(Scenario scenario) {
		
		TtCreateBraessPopulation popCreator = 
				new TtCreateBraessPopulation(scenario.getPopulation(), scenario.getNetwork());
		popCreator.setNumberOfPersons( 3600 );
		
		popCreator.createPersons(INIT_WITH_ALL_ROUTES ? TtCreateBraessPopulation.InitRoutes.ALL
				: TtCreateBraessPopulation.InitRoutes.NONE, INIT_PLAN_SCORE);
		
		if (WRITE_INITIAL_FILES) popCreator.writePopulation(INIT_FILE_DIR + "plans3600.xml");
	}

	private void createSignals(Scenario scenario) {

		TtCreateBraessSignals signalsCreator = new TtCreateBraessSignals(scenario);
		signalsCreator.setLaneType( LANE_TYPE );
		signalsCreator.setSignalType( SIGNAL_TYPE );
		signalsCreator.createSignals();
		
		if (WRITE_INITIAL_FILES) signalsCreator.writeSignalFiles(INIT_FILE_DIR);
	}

	private void createRunNameAndOutputDir(Scenario scenario) {

		Config config = scenario.getConfig();
		
		// get the current date in format "yyyy-mm-dd"
		Calendar cal = Calendar.getInstance ();
		// this class counts months from 0, but days from 1
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-" 
				+ monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);
		
		String runName = date;

		runName += "_" + scenario.getPopulation().getPersons().size() + "p";
		if (INIT_WITH_ALL_ROUTES){
			runName += "_initAllRoutes-sel1+3";
			if (INIT_PLAN_SCORE != null)
				runName += "-score" + INIT_PLAN_SCORE;
		}

		runName += "_" + config.controler().getLastIteration() + "it";

		// create info about the different possible travel times
		Link middleLink = scenario.getNetwork().getLinks()
				.get(Id.createLinkId("3_4"));
		Link slowLink = scenario.getNetwork().getLinks()
				.get(Id.createLinkId("3_5"));
		// note: use link 3_5 because it always exists
		if (middleLink == null){
			runName += "_woZ";
		} else {
			int fastTT = (int)Math.ceil(middleLink.getLength()
					/ middleLink.getFreespeed());
			int slowTT = (int)Math.ceil(slowLink.getLength()
					/ slowLink.getFreespeed());
			runName += "_" + fastTT + "-vs-" + slowTT;
		}
		
		// create info about capacity and link length
		runName += "_cap" + (int)slowLink.getCapacity();
		if (slowLink.getLength() != 200)
			runName += "_l" + (int)slowLink.getLength() + "m";
		
		if (scenario.getNetwork().getNodes().containsKey(Id.createNodeId(23))){
			runName += "_inflow";
			
			Link inflowLink = scenario.getNetwork().getLinks()
					.get(Id.createLinkId("2_23"));
			if (inflowLink.getLength() != 7.5)
				runName += "L" + inflowLink.getLength();
		}
		
		StrategySettings[] strategies = config.strategy().getStrategySettings()
				.toArray(new StrategySettings[0]);
		for (int i = 0; i < strategies.length; i++) {
			double weight = strategies[i].getWeight();
			if (weight != 0.0){
				String name = strategies[i].getStrategyName();
				runName += "_" + name + weight;
				if (name.equals(DefaultStrategy.ReRoute.toString())){
					runName += "_tbs"
							+ config.travelTimeCalculator().getTraveltimeBinSize();
				}
				if (name.equals(DefaultSelector.ChangeExpBeta.toString())){
					runName += "_beta" + (int)config.planCalcScore().getBrainExpBeta();
				}
			}
		}
		
		if (SIGMA != 0.0)
			runName += "_sigma" + SIGMA;
		if (config.planCalcScore().getMonetaryDistanceRateCar() != 0.0)
			runName += "_distCost"
					+ config.planCalcScore().getMonetaryDistanceRateCar();

		if (LANE_TYPE.equals(LaneType.TRIVIAL)) {
			runName += "_trivialLanes";
		}
		else if (LANE_TYPE.equals(LaneType.REALISTIC)){
			runName += "_lanes";
		}

		if (config.controler().isLinkToLinkRoutingEnabled())
			runName += "_link2link";
		else
			runName += "_node2node";

		if (ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME,
				SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			runName += "_" + SIGNAL_TYPE;
		}

		String outputDir = DgPaths.RUNSSVN + "braess/withSignals/" + runName + "/"; //TODO change this, e.g. when considering tolls
//		outputDir = "/Users/nagel/kairuns/braess/output";

		config.controler().setOutputDirectory(outputDir);
		log.info("The output will be written to " + outputDir);
	}

	public static void main(String[] args) {
		new RunBraessSimulation().prepareRunAndAnalyze();
	}
}
