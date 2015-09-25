package playground.tschlenther.Link2Link;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * @author tschlenther
 *
 *this class is not finished yet! 
 *basically it is supposed to test if agents "see" a different route in a network when signals are enabled
 *using Link2Link routing o default "Node2Node" routing.
 *
 *therefore a network with a fork is created (look at Link2LinkTestNetworkCreator for details) *
 *
 */

public class Link2LinkTestRunner {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private static String OUTPUT_DIR = "";

	private int NUMBER_OF_PERSONS = 15000;
	
//	private EventsManager events = EventsUtils.createEventsManager();
	
	@Test
	public void testLink2Link(){
		
		RunSettings runSettings = new RunSettings();

		createOutputDir(runSettings);
		TsAnalyzeLink2Link handler = new TsAnalyzeLink2Link(runSettings);
		prepareAndRun(runSettings, handler);
		
		runSettings.setUseLink2Link(true);
		runSettings.setUseSignals(true);
		createOutputDir(runSettings);
		handler.setToNextRun(runSettings);
		prepareAndRun(runSettings, handler);
		
		handler.writeResults();
		
		
	}

	private void prepareAndRun(RunSettings runSettings, TsAnalyzeLink2Link handler) {
		Config config = defineConfig(runSettings);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		Controler controler = new Controler(scenario);

		if ((boolean) signalConfigGroup.isUseSignalSystems()) {
			// add the signals module if signal systems are use
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(signalConfigGroup).loadSignalsData());
			controler.addOverridingModule(new SignalsModule());
		}

		if (config.controler().isLinkToLinkRoutingEnabled()) {
			// add the module for link to link routing if enabled
			controler.addOverridingModule(new InvertedNetworkTripRouterFactoryModule());
		}
		new Link2LinkTestNetworkCreator(scenario, runSettings.isUseLanes(), runSettings.isUseSignals()).createNetwork();
		this.createPersons(scenario);

		controler.getEvents().addHandler(handler);
		controler.run();

		handler.saveRunResults();
	}

	private void createOutputDir(RunSettings settings) {
		OUTPUT_DIR = utils.getOutputDirectory();
		OUTPUT_DIR += "lanes-" + settings.isUseLanes() + "-signals-" + settings.isUseSignals() + "-l2l-" + settings.isUseLink2Link();
	}

	/*currently most of content is copied from RunBraessSimulation
	 * 
	 */
	private static Config defineConfig(RunSettings settings) {
		Config config = ConfigUtils.createConfig();

		// set number of iterations
		config.controler().setLastIteration( 2 );

		// able or enable signals and lanes
		config.qsim().setUseLanes( settings.isUseLanes() );
		if(settings.isUseSignals()){
			SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME,SignalSystemsConfigGroup.class);
			signalConfigGroup.setUseSignalSystems(true);
		}
		
		// set brain exp beta
		config.planCalcScore().setBrainExpBeta( 20 );

		// choose between link to link and node to node routing
		if(settings.isUseLink2Link()){
		config.controler().setLinkToLinkRoutingEnabled( true );
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		}
		
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
			strat.setWeight( 0.1 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.ChangeExpBeta.toString() );
			strat.setWeight( 0.9 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}
		
		config.controler().setOutputDirectory(OUTPUT_DIR);

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.failIfDirectoryExists);		
		// note: the output directory is defined in createOutputDir(...) after all adaptations are done
		
		//set StuckTime
		config.qsim().setStuckTime(3600 * 10.);

		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(8 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);
		
		return config;
	}
	
	private void createPersons(Scenario scenario) {
			Population population = scenario.getPopulation();
			
			for (int i = 0; i < NUMBER_OF_PERSONS; i++) {
			
				// create a person and a plan container
				Person person = population.getFactory().createPerson(Id.createPersonId(i));
				Plan plan = population.getFactory().createPlan();

				// add a start activity at Link1
				Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId("Link1"));
		
				// 	8:00 am. plus i seconds
				startAct.setEndTime(8 * 3600 + i);
		
				plan.addActivity(startAct);

				// add a leg
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addLeg(leg);		

				// add a drain activity at Link7
				Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId("Link7"));
				plan.addActivity(drainAct);

				// store information in population
				person.addPlan(plan);
				population.addPerson(person);
			}
	}
	
}