package scenarios.braess.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
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
import org.matsim.signals.data.SignalsData;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.DgPaths;
import scenarios.braess.analysis.TtBraessControlerListener;
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
	
	private final String DATE = "2015-06-09";
	
	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private final double SIGMA = 0.0;
	
	private final String INPUT_DIR = DgPaths.SHAREDSVN
			+ "projects/cottbus/data/scenarios/braess_scenario/";
	
	private final long CAP_MAIN = 1800; // [veh/h]
	private final long CAP_FIRST_LAST = 3600; // [veh/h]
	
	/* population parameter */
	private int NUMBER_OF_PERSONS = 3600;
	private boolean SAME_START_TIME = false;
	private boolean INIT_WITH_ALL_ROUTES = true;
	
	private boolean SIMULATE_INFLOW_CAP = false;
	

	private void prepareRunAndAnalyse() {
		log.info("Starts running the simulation from input directory " + INPUT_DIR);
		
		// prepare config and scenario		
		Config config = defineConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		adaptNetwork(scenario);
		createPopulation(scenario);
		createRunNameAndOutputDir(scenario);
		if (config.scenario().isUseSignalSystems()){
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, 
					new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		}
		
		// prepare the controller
		Controler controler = new Controler(scenario);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		if (config.scenario().isUseSignalSystems()){
			// add the signals module if signal systems are used
			controler.addOverridingModule(new SignalsModule());
		}
		// adapt sigma for randomized routing
		final RandomizingTimeDistanceTravelDisutility.Builder builder = new RandomizingTimeDistanceTravelDisutility.Builder();
		builder.setSigma(this.SIGMA);
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

		// set network and lane properties
		config.network().setInputFile(INPUT_DIR + "basicNetwork.xml");
		config.scenario().setUseLanes( false );
		config.network().setLaneDefinitionsFile(
				INPUT_DIR + "lanes" + CAP_MAIN + "-" + CAP_FIRST_LAST + ".xml");

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
			strat.setDisableAfter( 50 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.SelectRandom.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( 300 );
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
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.KeepLastSelected.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() - 50 );
			config.strategy().addStrategySettings(strat);
		}

		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize( 0 );
		
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
		
		config.qsim().setStuckTime(3600 * 10.);
		
		return config;
	}

	private void adaptNetwork(Scenario scenario) {	
		Network net = scenario.getNetwork();

		// set travel times at the links (by adapting free speed)
		// note: you only have to adapt the denominator (to the desired travel time), 
		// because all links have length 200m in the basic network
		net.getLinks().get(Id.createLinkId("0_1")).setFreespeed(200 / 1);
		net.getLinks().get(Id.createLinkId("1_2")).setFreespeed(200 / 1);
		net.getLinks().get(Id.createLinkId("2_3")).setFreespeed(200 / 10);
		net.getLinks().get(Id.createLinkId("2_4")).setFreespeed(200 / 20);
		net.getLinks().get(Id.createLinkId("3_4")).setFreespeed(200 / 1);
		net.getLinks().get(Id.createLinkId("3_5")).setFreespeed(200 / 20);
		net.getLinks().get(Id.createLinkId("4_5")).setFreespeed(200 / 10);
		net.getLinks().get(Id.createLinkId("5_6")).setFreespeed(200 / 1);
	
		// adapt capacity on all links		
		for (Link l : net.getLinks().values()){
			if (l.getId().equals(Id.createLinkId("0_1")) || 
					l.getId().equals(Id.createLinkId("1_2")) ||
					l.getId().equals(Id.createLinkId("5_6")) )
				l.setCapacity(CAP_FIRST_LAST);
			else
				l.setCapacity(CAP_MAIN);
		}
		
		// extend network, if inflow capacity should be simulated
		if (SIMULATE_INFLOW_CAP) {
			NetworkFactory fac = net.getFactory();

			// create 2 new nodes
			net.addNode(fac.createNode(Id.createNodeId(7),
					scenario.createCoord(250, 250)));
			net.addNode(fac.createNode(Id.createNodeId(8),
					scenario.createCoord(250, 150)));

			// remove former links 2_3 and 2_4
			net.getLinks().get(Id.createLinkId("2_3")).setFreespeed(0.1);
			net.getLinks().get(Id.createLinkId("2_4")).setFreespeed(0.1);
			/* the following code creates an UnsupportedOperationException
			because the map is an UnmodifiableMap */
//			net.getLinks().remove(Id.createLinkId("2_3")); 
//			net.getLinks().remove(Id.createLinkId("2_4"));

			// create 4 new links
			Link l = fac.createLink(Id.createLinkId("2_7"),
					net.getNodes().get(Id.createNodeId(2)),
					net.getNodes().get(Id.createNodeId(7)));
			l.setCapacity(CAP_MAIN);
			l.setLength(10);
			l.setFreespeed(10 / 1);
			net.addLink(l);
			
			l = fac.createLink(Id.createLinkId("7_3"),
					net.getNodes().get(Id.createNodeId(7)),
					net.getNodes().get(Id.createNodeId(3)));
			l.setCapacity(CAP_MAIN);
			l.setLength(200);
			l.setFreespeed(200 / 999);
			net.addLink(l);

			l = fac.createLink(Id.createLinkId("2_8"),
					net.getNodes().get(Id.createNodeId(2)),
					net.getNodes().get(Id.createNodeId(8)));
			l.setCapacity(CAP_MAIN);
			l.setLength(10);
			l.setFreespeed(10 / 1);
			net.addLink(l);
			
			l = fac.createLink(Id.createLinkId("8_4"),
					net.getNodes().get(Id.createNodeId(8)),
					net.getNodes().get(Id.createNodeId(4)));
			l.setCapacity(CAP_MAIN);
			l.setLength(200);
			l.setFreespeed(200 / 1999);
			net.addLink(l);
		}
	}

	private void createPopulation(Scenario scenario) {
		
		TtCreateBraessPopulation popCreator = 
				new TtCreateBraessPopulation(scenario.getPopulation(), scenario.getNetwork());
		popCreator.createPersons(NUMBER_OF_PERSONS, SAME_START_TIME, INIT_WITH_ALL_ROUTES);
	}

	private void createRunNameAndOutputDir(Scenario scenario) {

		Config config = scenario.getConfig();
		
		String runName = this.DATE;

		runName += "_" + this.NUMBER_OF_PERSONS + "p";
		if (this.SAME_START_TIME)
			runName += "_sameStart";
		if (this.INIT_WITH_ALL_ROUTES)
			runName += "_initAllRoutes";

		runName += "_" + config.controler().getLastIteration() + "it";

		Link middleLink = scenario.getNetwork().getLinks()
				.get(Id.createLinkId("3_4"));
		runName += "_cap" + middleLink.getCapacity();
		runName += "_ttMid" + middleLink.getLength()
				/ middleLink.getFreespeed() + "s";

		if (SIMULATE_INFLOW_CAP)
			runName += "_inflowCap";
		
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

		if (this.SIGMA != 0.0)
			runName += "_sigma" + this.SIGMA;
		if (config.planCalcScore().getMonetaryDistanceCostRateCar() != 0.0)
			runName += "_distCost"
					+ config.planCalcScore().getMonetaryDistanceCostRateCar();

		if (config.scenario().isUseLanes()) {
			String[] lanesInfoSplit = config.network().getLaneDefinitionsFile()
					.split("\\.")[0].split("/");
			String lanesFileName = lanesInfoSplit[lanesInfoSplit.length - 1];
			runName += "_" + lanesFileName;
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
