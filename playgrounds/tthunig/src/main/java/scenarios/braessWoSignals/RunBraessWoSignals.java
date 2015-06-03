package scenarios.braessWoSignals;

import java.io.File;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.analysis.AnalyzeBraessSimulation;
import playground.dgrether.koehlerstrehlersignal.braessscenario.TtCreateBraessPopulation;

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
	
	private final String DATE = "2015-06-03";
	
	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private final double SIGMA = 0.0;
	
	private final String INPUT_DIR = DgPaths.SHAREDSVN
			+ "studies/tthunig/scenarios/BraessWoSignals/";
	
	private final long CAP_MAIN = 1800; // [veh/h]
	private final long CAP_FIRST_LAST = 3600; // [veh/h]
	
	/* population parameter */
	private int NUMBER_OF_PERSONS = 60;
	private boolean SAME_START_TIME = true;
	private boolean INIT_WITH_ALL_ROUTES = true;
	

	private void prepareAndRunAndAnalyse() {
		// write some information
		log.info("Starts running the simulation from input directory " + INPUT_DIR);
		
		// prepare the simulation		
		Config config = defineConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		adaptNetwork(scenario);
		createPopulation(scenario);
		createRunNameAndOutputDir(scenario);

		// prepare the controller
		Controler controler = new Controler(scenario);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		// adapt sigma for randomized routing
		final RandomizingTimeDistanceTravelDisutility.Builder builder = new RandomizingTimeDistanceTravelDisutility.Builder();
		builder.setSigma(this.SIGMA);
		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				bindTravelDisutilityFactory().toInstance(builder);
			}
		});

		// run the simulation
		controler.run();

		// analyze the simulation
		String analyzeDir = config.controler().getOutputDirectory() + "analysis/";
		new File(analyzeDir).mkdir();
		AnalyzeBraessSimulation analyzer = new AnalyzeBraessSimulation(
				config.controler().getOutputDirectory(), 
				config.controler().getLastIteration(), analyzeDir);
		// if detailed information is available
		if (config.controler().getWriteEventsInterval() == 1)
			// analyze all iterations in terms of route choice and travel time
			analyzer.analyzeAllIt();
		// analyze the last iteration more detailed
		analyzer.analyzeLastIt();
	}

	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		// set network and lane properties
		config.network().setInputFile(INPUT_DIR + "basicNetwork.xml");
		config.scenario().setUseLanes( false );
		config.network().setLaneDefinitionsFile(
				INPUT_DIR + "lanes" + CAP_MAIN + "-" + CAP_FIRST_LAST + ".xml");

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
		
		// set number of iterations
		config.controler().setLastIteration( 100 );

		// define strategies:
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultStrategy.ReRoute.toString() );
			strat.setWeight( 0.1 ) ;
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
			strat.setWeight( 0.9 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.BestScore.toString() );
			strat.setWeight( 0.0 ) ;
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

		// adapt events writing interval
		// use 1 if you like to have a detailed analysis.
		config.controler().setWriteEventsInterval( 1 );
		
		config.controler().setWritePlansInterval( config.controler().getLastIteration() );
		
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);
		
		return config;
	}

	private void adaptNetwork(Scenario scenario) {		
		// set travel times at the links (by adapting free speed)
		// note: you only have to adapt the denominator, 
		// because all links have length 200m in the basic network
		scenario.getNetwork().getLinks().get(Id.create(0, Link.class)).setFreespeed(200 / 1);
		scenario.getNetwork().getLinks().get(Id.create(1, Link.class)).setFreespeed(200 / 1);
		scenario.getNetwork().getLinks().get(Id.create(2, Link.class)).setFreespeed(200 / 10);
		scenario.getNetwork().getLinks().get(Id.create(3, Link.class)).setFreespeed(200 / 20);
		scenario.getNetwork().getLinks().get(Id.create(4, Link.class)).setFreespeed(200 / 1);
		scenario.getNetwork().getLinks().get(Id.create(5, Link.class)).setFreespeed(200 / 20);
		scenario.getNetwork().getLinks().get(Id.create(6, Link.class)).setFreespeed(200 / 10);
		scenario.getNetwork().getLinks().get(Id.create(7, Link.class)).setFreespeed(200 / 1);
	
		// adapt capacity on all links		
		for (Link l : scenario.getNetwork().getLinks().values()){
			if (l.getId().equals(Id.create(0, Link.class)) || 
					l.getId().equals(Id.create(1, Link.class)) ||
					l.getId().equals(Id.create(7, Link.class)) )
				l.setCapacity(CAP_FIRST_LAST);
			else
				l.setCapacity(CAP_MAIN);
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
				.get(Id.create(4, Link.class));
		runName += "_cap" + middleLink.getCapacity();
		runName += "_ttMid" + middleLink.getLength()
				/ middleLink.getFreespeed() + "s";

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
			String lanesFile = config.network().getLaneDefinitionsFile();
			String[] lanesDotSlashSplit = (lanesFile.split("\\."))[0].split("/");
			String lanesFileName = lanesDotSlashSplit[lanesDotSlashSplit.length - 1];
			runName += "_" + lanesFileName;
		}

		if (config.controler().isLinkToLinkRoutingEnabled())
			runName += "_link2link";
		else
			runName += "_node2node";

		String outputDir = DgPaths.RUNSSVN + "braess/" + runName + "/";
//		outputDir = "/Users/nagel/kairuns/braess/output";

		config.controler().setOutputDirectory(outputDir);
		log.info("The output will be written to " + outputDir);
	}

	public static void main(String[] args) {
		new RunBraessWoSignals().prepareAndRunAndAnalyse();
	}
}
