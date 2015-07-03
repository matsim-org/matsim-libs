package scenarios.braess.analysis;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import scenarios.braess.createInput.TtCreateBraessPopulation;

/**
 * This class tests the functionality of the getTotalTT method in the class
 * TtAnalyzeBraessRouteDistributionAndTT. The network used is the basic Braess
 * scenario with unlimited capacity, the number of persons traveling through the
 * scenario and the travel time per link can be varied.
 * 
 * @author Tilmann Schlenther
 * 
 */
public class TtAnalyzeBraessRouteDistributionAndTTTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final int NUMBER_OF_PERSONS = 1;
	
	private int TTPerLink = 3;
	
	private boolean agentsToStuck = false;
	
	private String outputdir;
	
	
	/**
	 * Test method for {@link scenarios.braess.analysis.TtAnalyzeBraessRouteDistributionAndTT#getTotalTT()}.
	 */	
	@Test
	public void testGetTotalTT() {
		outputdir = utils.getOutputDirectory() + "/Test_LinkTT" + TTPerLink;
		//TTperLink must not be 0;
		if(TTPerLink == 0) TTPerLink = 1;
		runSimulation(agentsToStuck);
		EventsManager events = EventsUtils.createEventsManager();
		TtAnalyzeBraessRouteDistributionAndTT handler = new TtAnalyzeBraessRouteDistributionAndTT();
		events.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(outputdir+"/ITERS/it.0/0.events.xml");
		
		
		/*expectedTravelTime depends on variable TTperLink
		*LinkTravelTime on Link 0_1 always is 1 sec
		*LinkTravelTime on Links between Node 1 and 5 is TTperLink +1 (MATSim's TimeStep-logic)
		*=> so you get 4 extra seconds in this scenario
		*LinkTraveltime on Link 5_6 is equivalent to TTperLink
		*/
		
		Double expectedTravelTime = (double) (1+5*TTPerLink+4)*NUMBER_OF_PERSONS;
		
		Assert.assertEquals("iteration 0: TT stimmt nicht", expectedTravelTime , handler.getTotalTT(), MatsimTestUtils.EPSILON);
		events.resetHandlers(0);
		reader.readFile(outputdir+"/ITERS/it.1/1.events.xml");
		Assert.assertEquals("iteration 1: TT stimmt nicht", expectedTravelTime , handler.getTotalTT(), MatsimTestUtils.EPSILON);
		events.resetHandlers(0);
		reader.readFile(outputdir+"/ITERS/it.2/2.events.xml");
		Assert.assertEquals("iteration 2: TT stimmt nicht", expectedTravelTime , handler.getTotalTT(), MatsimTestUtils.EPSILON);
		
	}
	

	private void runSimulation(boolean agentsToStuck) {
		
		// prepare config and scenario		
		Config config = defineConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		adaptNetwork(scenario, agentsToStuck);
		createPopulation(scenario);
		
		// prepare the controller
		Controler controler = new Controler(scenario);
		
		// run the simulation
		controler.run();
		
	}

	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(utils.getInputDirectory()+"basicNetwork.xml");
		
		// set number of iterations
		config.controler().setLastIteration( 2 );

		// make agents keep their initial plan selected
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.KeepLastSelected.toString() );
			strat.setWeight( 1) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}
		
		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize( 1 );
		
		//write out plans and events every iteration
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		
		//set StuckTime
//		config.qsim().setStuckTime(20);
		
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);
		config.controler().setOutputDirectory(outputdir);

		return config;
	}

	private void adaptNetwork(Scenario scenario , boolean agentsToStuck) {		
		// set the links' travel times (by adapting free speed) and capacity (to unlimited)
		
		for(Link l : scenario.getNetwork().getLinks().values()){
			// modify the capacity and/or length of the middle link to let agents stuck
			if(agentsToStuck && l.getId().equals(Id.createLinkId("3_4"))){
						scenario.getConfig().qsim().setStuckTime(TTPerLink + 1);
						scenario.getConfig().qsim().setRemoveStuckVehicles(true);
						l.setFreespeed(200/(TTPerLink+10));
						l.setCapacity(1);				
			}
			else if (agentsToStuck && l.getId().equals(Id.createLinkId("4_5"))){
				l.setCapacity(1);
			}	
			else{	
				l.setCapacity(999999);
				l.setFreespeed(200/TTPerLink);
			}
		}
	}
	
	private void createPopulation(Scenario scenario) {	
		
		// initialize all agents with only one route (the middle route)
		int numberOfInitialRoutes = 1;
		
		TtCreateBraessPopulation popCreator = new TtCreateBraessPopulation(scenario.getPopulation(), scenario.getNetwork());
		popCreator.setNumberOfPersons(NUMBER_OF_PERSONS);
		popCreator.createPersons( numberOfInitialRoutes , null);
	}

	

}