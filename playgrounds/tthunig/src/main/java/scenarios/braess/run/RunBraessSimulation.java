package scenarios.braess.run;

import java.util.Calendar;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
//import org.matsim.contrib.otfvis.OTFVisModule;
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
import scenarios.braess.analysis.TtBraessControlerListener;
import scenarios.braess.createInput.TtCreateBraessNetworkAndLanes;
import scenarios.braess.createInput.TtCreateBraessPopulation;

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
	
	private final String INPUT_DIR = DgPaths.SHAREDSVN
			+ "projects/cottbus/data/scenarios/braess_scenario/";
	
	/* population parameter */
	private final int NUMBER_OF_PERSONS = 3600;
	// If true, all agents start their trip at 8 am. If not, the agents start
	// after each other in one second gaps, the first one at 8 am.
	private final boolean SAME_START_TIME = false;
	// If false, agents are initialized without any routes. If true, with all
	// three possible routes.
	private final boolean INIT_WITH_ALL_ROUTES = true;
	// initial score for all initial plans
	private final Double INIT_PLAN_SCORE = 110.;
	
	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private final double SIGMA = 0.0;	
		

	private void prepareRunAndAnalyse() {
		log.info("Starts running the simulation from input directory " + INPUT_DIR);
		
		// prepare config and scenario		
		Config config = defineConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		createNetwork(scenario);
		createPopulation(scenario);
		createRunNameAndOutputDir(scenario);
		if (config.scenario().isUseSignalSystems()){
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, 
					new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		}
		
		// prepare the controller
		Controler controler = new Controler(scenario);
//		controler.addOverridingModule(new OTFVisModule());
		
		if (config.scenario().isUseSignalSystems()){
			// add the signals module if signal systems are used
			controler.addOverridingModule(new SignalsModule());
		}
		
		if (config.controler().isLinkToLinkRoutingEnabled()){
			// add the module for link to link routing if enabled
			controler.addOverridingModule(new InvertedNetworkTripRouterFactoryModule());
		}
		
		// adapt sigma for randomized routing
		final RandomizingTimeDistanceTravelDisutility.Builder builder = new RandomizingTimeDistanceTravelDisutility.Builder();
		builder.setSigma(SIGMA);
		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				bindTravelDisutilityFactory().toInstance(builder);
			}
		});
		
		// add a controller listener to analyze results
		controler.addControlerListener(new TtBraessControlerListener(scenario));
		
		// run the simulation
		controler.run();
	}

	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		// choose whether lanes should be used or not
		config.scenario().setUseLanes( true );

		// set number of iterations
		config.controler().setLastIteration( 100 );

		// set signal files
		config.scenario().setUseSignalSystems( false );
		config.signalSystems().setSignalControlFile(INPUT_DIR + "signalControl_Green.xml");
		config.signalSystems().setSignalGroupsFile(INPUT_DIR + "signalGroups.xml");
		config.signalSystems().setSignalSystemFile(INPUT_DIR + "signalSystems.xml");
		
		// set brain exp beta
		config.planCalcScore().setBrainExpBeta( 20 );

		// choose between link to link and node to node routing
		config.controler().setLinkToLinkRoutingEnabled( true );
		
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		config.travelTimeCalculator().setCalculateLinkTravelTimes(true);
		
		// set travelTimeBinSize
		config.travelTimeCalculator().setTraveltimeBinSize( 900 );
		
		config.travelTimeCalculator().setTravelTimeCalculatorType(
				TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may 2015
		
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
			strat.setWeight( 0.1 ) ;
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
			strat.setWeight( 0.9 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}

		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize( 0 );
		
		config.qsim().setStuckTime(3600 * 10.);
		
		// adapt monetary distance cost rate
		// (should be negative. use -12.0 to balance time [h] and distance [m].
		// use -0.00015 to approximately balance the utility of travel time and
		// distance in this scenario.
		// use -0.0 to use only time.)
		config.planCalcScore().setMonetaryDistanceCostRateCar( 0.0 );

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );		
		// note: the output directory is defined in createRunNameAndOutputDir(...) after all adaptations are done
		
		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(true);

		config.controler().setWriteEventsInterval( config.controler().getLastIteration() );
		config.controler().setWritePlansInterval( config.controler().getLastIteration() );
		
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);
		
		return config;
	}

	private void createNetwork(Scenario scenario) {	
		
		TtCreateBraessNetworkAndLanes netCreator = new TtCreateBraessNetworkAndLanes(scenario);
		netCreator.createNetwork();
		
		if (scenario.getConfig().scenario().isUseLanes()){
			netCreator.createLanes();
		}
	}

	private void createPopulation(Scenario scenario) {
		
		TtCreateBraessPopulation popCreator = 
				new TtCreateBraessPopulation(scenario.getPopulation(), scenario.getNetwork());
		popCreator.createPersons(NUMBER_OF_PERSONS, SAME_START_TIME, INIT_WITH_ALL_ROUTES, INIT_PLAN_SCORE);
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

		runName += "_" + NUMBER_OF_PERSONS + "p";
		if (SAME_START_TIME)
			runName += "_sameStart";
		if (INIT_WITH_ALL_ROUTES){
			runName += "_initAllRoutes-sel1+3";
			if (INIT_PLAN_SCORE != null)
				runName += "-score" + INIT_PLAN_SCORE;
		}

		runName += "_" + config.controler().getLastIteration() + "it";

		Link middleLink = scenario.getNetwork().getLinks()
				.get(Id.createLinkId("3_4"));
		runName += "_cap" + middleLink.getCapacity();
		runName += "_ttMid" + middleLink.getLength()
				/ middleLink.getFreespeed() + "s";

		if (middleLink.getLength() != 200)
			runName += "_l" + middleLink.getLength() + "m";
		
		if (scenario.getNetwork().getNodes().containsKey(Id.createNodeId(7)))
			runName += "_inflowCapZ";
		
		StrategySettings[] strategies = config.strategy().getStrategySettings()
				.toArray(new StrategySettings[0]);
		for (int i = 0; i < strategies.length; i++) {
			if (strategies[i].getWeight() != 0.0)
				runName += "_" + strategies[i].getStrategyName()
						+ strategies[i].getWeight();
		}
		runName += "_beta" + config.planCalcScore().getBrainExpBeta();

		runName += "_ttBinSize"
				+ config.travelTimeCalculator().getTraveltimeBinSize();

		if (SIGMA != 0.0)
			runName += "_sigma" + SIGMA;
		if (config.planCalcScore().getMonetaryDistanceCostRateCar() != 0.0)
			runName += "_distCost"
					+ config.planCalcScore().getMonetaryDistanceCostRateCar();

		if (config.scenario().isUseLanes()) {
			runName += "_lanes";
		}

		if (config.controler().isLinkToLinkRoutingEnabled())
			runName += "_link2link";
		else
			runName += "_node2node";
		
		if (config.scenario().isUseSignalSystems()){
			String[] signalsInfoSplit = config.signalSystems().getSignalControlFile()
					.split("\\.")[0].split("_");
			String signalsInfo = signalsInfoSplit[signalsInfoSplit.length - 1];
			runName += "_signals" + signalsInfo;
		}

		String outputDir = DgPaths.RUNSSVN + "braess/" + runName + "/";
//		outputDir = "/Users/nagel/kairuns/braess/output";

		config.controler().setOutputDirectory(outputDir);
		log.info("The output will be written to " + outputDir);
	}

	public static void main(String[] args) {
		new RunBraessSimulation().prepareRunAndAnalyse();
	}
}
