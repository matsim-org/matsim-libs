package scenarios.braessWoSignals;

import java.io.File;

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
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;
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
	
	private final String DATE = "2015-06-01";
	
	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private final double SIGMA = 0.0;
	
	private final String INPUT_DIR = DgPaths.SHAREDSVN
			+ "studies/tthunig/scenarios/BraessWoSignals/";
	
	private final long CAP_MAIN = 1800;
	private final long CAP_FIRST_LAST = 3600;
	

	private void prepareAndRunAndAnalyse() {
		// write some information
		log.info("Starts running the simulation from input directory " + INPUT_DIR);
		
		// prepare the simulation		
		Config config = adaptConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		adaptNetwork(scenario);
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

	private Config adaptConfig() {
		// read config file
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, INPUT_DIR + "basicConfig.xml");

		// adapt plans file. (adapt number of agents here)
		config.plans().setInputFile(INPUT_DIR
				+ "plans" + 3600 + "SameStartTimeAllRoutes.xml");

		// set network and lane properties
		config.network().setInputFile(INPUT_DIR + "basicNetwork.xml");
		config.scenario().setUseLanes(false);
		config.network().setLaneDefinitionsFile(
				INPUT_DIR + "lanes" + CAP_MAIN + "-" + CAP_FIRST_LAST + ".xml");

		config.planCalcScore().setBrainExpBeta(20);

		config.controler().setLinkToLinkRoutingEnabled(true);
		
		// adapt travelTimeBinSize and travelTimeCalculatorType
		config.travelTimeCalculator().setTraveltimeBinSize(1);
		config.travelTimeCalculator().setTravelTimeCalculatorType(
				TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may 2015
		
		// adapt number of iterations
		config.controler().setLastIteration(1000);

		// remove all strategies possibly defined in basicConfigFile:
		config.strategy().clearStrategySettings(); // functionality available since 2015/05/31. kai
		
		// define strategies:
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultStrategy.ReRoute.toString() );
			strat.setWeight( 0.1 ) ;
			strat.setDisableAfter(50);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.SelectRandom.toString() );
			strat.setWeight( 0.3 ) ;
			strat.setDisableAfter(300);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.ChangeExpBeta.toString() );
			strat.setWeight( 0.3 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() - 50 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.BestScore.toString() );
			strat.setWeight( 0.3 ) ;
			config.strategy().addStrategySettings(strat);
		}

		// 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize(0);
		
		// adapt monetary distance cost rate
		// (should be negative. use -12.0 to balance time [h] and distance [m].
		// use -0.00015 to approximately balance the utility of travel time and
		// distance in this scenario.
		// use -0.0 to use only time.)
		config.planCalcScore().setMonetaryDistanceCostRateCar(0.0);

		// note: the output directory is set in createRunNameAndOutputDir(...) after all adaptations are done
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );		
	
		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(true);

		// adapt events writing interval
		// use 1 if you like to have a detailed analysis.
		config.controler().setWriteEventsInterval(1);
		
		return config;
	}

	private void adaptNetwork(Scenario scenario) {		
		// set travel times at the links (by adapting free speed)
		// note: you only have to adapt the denominator, 
		// because all links have length 200m in the basic network 
		scenario.getNetwork().getLinks().get(Id.create(1, Link.class)).setFreespeed(200 / 1);
		scenario.getNetwork().getLinks().get(Id.create(2, Link.class)).setFreespeed(200 / 10);
		scenario.getNetwork().getLinks().get(Id.create(3, Link.class)).setFreespeed(200 / 20);
		scenario.getNetwork().getLinks().get(Id.create(4, Link.class)).setFreespeed(200 / 1);
		scenario.getNetwork().getLinks().get(Id.create(5, Link.class)).setFreespeed(200 / 20);
		scenario.getNetwork().getLinks().get(Id.create(6, Link.class)).setFreespeed(200 / 10);
		scenario.getNetwork().getLinks().get(Id.create(7, Link.class)).setFreespeed(200 / 1);
	
		// adapt capacity on all links		
		for (Link l : scenario.getNetwork().getLinks().values()){
			if (l.getId().equals(Id.create(1, Link.class)) || 
					l.getId().equals(Id.create(7, Link.class)))
				l.setCapacity(CAP_FIRST_LAST);
			else
				l.setCapacity(CAP_MAIN);
		}
	}

	private void createRunNameAndOutputDir(Scenario scenario) {

		Config config = scenario.getConfig();

		String runName = this.DATE;

		// get plan information (numberOfAgents, start time, initialized routes)
		String plansFile = config.plans().getInputFile();
		String[] plansDotSlashSplit = (plansFile.split("\\."))[0].split("/");
		String plansFileName = plansDotSlashSplit[plansDotSlashSplit.length - 1];
		runName += "_" + plansFileName.substring(5); // adds the plans file name without 'plans'

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

		String outputDir = INPUT_DIR + "matsim-output/" + runName + "/";
		// outputDir = DgPaths.RUNSSVN + "braess/" + runName + "/";
		outputDir = "/Users/nagel/kairuns/braess/output";

		config.controler().setOutputDirectory(outputDir);
		log.info("The output will be written to " + outputDir);
	}

	public static void main(String[] args) {
		new RunBraessWoSignals().prepareAndRunAndAnalyse();
	}
}
