package scenarios.braess.analysis;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import scenarios.illustrative.braess.analysis.TtAnalyzeBraess;
import scenarios.illustrative.braess.createInput.TtCreateBraessPopulation;

/**
 * This class tests the functionality of the class
 * TtAnalyzeBraessRouteDistributionAndTT. The network used is the basic Braess
 * scenario with unlimited capacity, the number of persons traveling through the
 * scenario and the travel time per link can be varied.
 * 
 * @author Tilmann Schlenther, Gabriel Thunig
 * 
 */
public class TtAnalyzeBraessRouteDistributionAndTTTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private int personCount = 1;
	
	private int TTPerLink = 3;
	
	private boolean agentsToStuck = false;
	
	private String outputdir;
	
	/**
	 * Test method for {@link scenarios.illustrative.braess.analysis.TtAnalyzeBraess#calculateAvgRouteTTsByDepartureTime()}
	 * with 2 agents driving different routes and again with 5 agents driving the same route
	 * 
	 * @author Gabriel Thunig
	 */
	@Test
	public void testCalculateAvgRouteTTsByDepartureTime() {
		
		/* --------------- scenario 1 : two agents driving different routes --------------- */
		TtAnalyzeBraess handler = testAvgRouteScenario(2, TtCreateBraessPopulation.InitRoutes.ONLY_OUTER, 99999);
		
		int[] routeUsers = handler.getRouteUsers();
		int[] expectedRouteUsers = {1, 0, 1};
		Assert.assertArrayEquals(expectedRouteUsers, routeUsers);
		
		/*
		 * expectedTravelTime sums up like this:
		 * 1 sec for the termination of the starting Link
		 * 4 links to drive over
		 * 	per link: traveltime of the link
		 * 			  + 1 second because one timestep is necessary to transfer an agent to the next link
		 * - 1 second in total because its not necessary to transfer an agent on his destination-link
		 */
		Double expectedTravelTime = (double) (1+4*(TTPerLink+1)-1);
		Assert.assertEquals("Unexpected travel time on the upper route: ", expectedTravelTime, 
				handler.calculateAvgRouteTTsByDepartureTime().get(28800.0)[0], MatsimTestUtils.EPSILON);
		Assert.assertTrue("Unexpected travel time on the middle route: ", 
				Double.isNaN(handler.calculateAvgRouteTTsByDepartureTime().get(28800.0)[1]));
		Assert.assertEquals("Unexpected travel time on the lower route: ", expectedTravelTime, 
				handler.calculateAvgRouteTTsByDepartureTime().get(28800.0)[2], MatsimTestUtils.EPSILON);
		
		
		/* --------------- scenario 2 : five agents driving same route --------------- */
		handler = testAvgRouteScenario(5, TtCreateBraessPopulation.InitRoutes.ONLY_MIDDLE, 3600);
		
		routeUsers = handler.getRouteUsers();
		int[] expectedRouteUsers2 = {0, 5, 0};
		Assert.assertArrayEquals(expectedRouteUsers2, routeUsers);
		
		/*
		 * expectedTravelTime sums up like this:
		 * 1 sec for the termination of the starting Link
		 * 5 links to drive over
		 * 	 per link: traveltime of the link
		 * 			  + 1 second because one timestep is necessary to transfer an agent to the next link
		 * - 1 second in total because its not necessary to transfer an agent on his destination-link
		 * + 2 seconds in total as an average of delay caused by congestion summing up like this:
		 * 	 1.Driver has no delay
		 * 	 2.Driver has a delay of 1 second(because there are 3600 vehicles per hour allowed, so 1 per second)
		 * 	 3.Driver has a delay of 2 seconds(1 second + the delay of the previous vehicle as an addition)
		 * 	 4.Driver has a delay of 3 seconds(1+2)
		 * 	 5.Driver has a delay of 4 seconds(1+3)
		 * summed up 1+2+3+4=10 divided by the number of vehicles(5) there is an average delay of 2 seconds
		 * so: expectedTT(20) + (2)avarage delay is 22
		 * 
		 */
		expectedTravelTime = (double) (1+5*(TTPerLink+1)-1 + 2);
		Assert.assertTrue("Unexpected travel time on the upper route: ", 
				Double.isNaN(handler.calculateAvgRouteTTsByDepartureTime().get(28800.0)[0]));
		Assert.assertEquals("Unexpected travel time on the middle route: ", expectedTravelTime, 
				handler.calculateAvgRouteTTsByDepartureTime().get(28800.0)[1], MatsimTestUtils.EPSILON);
		Assert.assertTrue("Unexpected travel time on the lower route: ", 
				Double.isNaN(handler.calculateAvgRouteTTsByDepartureTime().get(28800.0)[2]));
	}
	
	/**
	 * Scenario where every person starts at the same time. 
	 * So there is only on resulting array with average travel times (with one double per route)
	 * 
	 * @param personCount number of persons
	 * @param initRoutes specifies which routes to use
	 * @param capacity of inner network
	 * @return returns the TtAnalyzeBraess-handler
	 */
	private TtAnalyzeBraess testAvgRouteScenario(int personCount, TtCreateBraessPopulation.InitRoutes initRoutes, int capacity) {
		this.TTPerLink = 3;
		this.personCount = personCount;
		outputdir = utils.getOutputDirectory() + "Test_AvgRouteTT" + personCount;
	
		// prepare config and scenario		
		Config config = defineConfig(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		adaptNetwork(scenario, capacity, agentsToStuck);
		
		createPopulation(scenario, initRoutes);
		changePopulationToSameStartTime(scenario, 28800.0);
		
		// prepare the controller
		Controler controler = new Controler(scenario);
		
		// run the simulation
		controler.run();
		
		EventsManager events = EventsUtils.createEventsManager();
		TtAnalyzeBraess handler = new TtAnalyzeBraess();
		events.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(outputdir+"/ITERS/it.0/0.events.xml");

//		//print the AvgRouteTTs of 8:00
//		Map<Double, double[]> map = handler.calculateAvgRouteTTsByDepartureTime();
//		for (Map.Entry<Double, double[]> entry : map.entrySet()) {
//			System.out.println("Entry");
//			System.out.println("Key: " + entry.getKey());
//			for (int i = 0; i < entry.getValue().length; i++) {
//				System.out.println("Value" + i + ": " + entry.getValue()[i]);
//			}
//			System.out.println();
//		}
		return handler;
	}
	
	/**
	 * Test method for {@link scenarios.illustrative.braess.analysis.TtAnalyzeBraess#getTotalTT()}.
	 * 
	 * @author Tilmann Schlenther
	 */	
	@Test
	public void testGetTotalTT() {
		this.TTPerLink = 3;
		this.personCount = 1;
		outputdir = utils.getOutputDirectory() + "Test_LinkTT" + TTPerLink;
		// prepare config and scenario		
		Config config = defineConfig(2);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		adaptNetwork(scenario, 99999, agentsToStuck);
		createPopulation(scenario, TtCreateBraessPopulation.InitRoutes.ONLY_MIDDLE);
		
		// prepare the controller
		Controler controler = new Controler(scenario);
		
		// run the simulation
		controler.run();
		EventsManager events = EventsUtils.createEventsManager();
		TtAnalyzeBraess handler = new TtAnalyzeBraess();
		events.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		
		
		/*expectedTravelTime depends on variable TTperLink
		*LinkTravelTime on Link 0_1 always is 1 sec
		*LinkTravelTime on Links between Node 1 and 5 is TTperLink +1 (MATSim's TimeStep-logic)
		*=> so you get 4 extra seconds in this scenario
		*LinkTraveltime on Link 5_6 is equivalent to TTperLink
		*/
		
		Double expectedTravelTime = (double) (1+5*TTPerLink+4)*personCount;
		
		reader.readFile(outputdir+"/ITERS/it.0/0.events.xml");
		Assert.assertEquals("iteration 0: TT stimmt nicht", expectedTravelTime , handler.getTotalTT(), MatsimTestUtils.EPSILON);
		events.resetHandlers(0);
		reader.readFile(outputdir+"/ITERS/it.1/1.events.xml");
		Assert.assertEquals("iteration 1: TT stimmt nicht", expectedTravelTime , handler.getTotalTT(), MatsimTestUtils.EPSILON);
		events.resetHandlers(0);
		reader.readFile(outputdir+"/ITERS/it.2/2.events.xml");
		Assert.assertEquals("iteration 2: TT stimmt nicht", expectedTravelTime , handler.getTotalTT(), MatsimTestUtils.EPSILON);
		
	}

	private Config defineConfig(int lastIterationNumber) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(utils.getInputDirectory()+"basicNetwork.xml");
		
		// set number of iterations
		config.controler().setLastIteration( lastIterationNumber );

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

	private void adaptNetwork(Scenario scenario, int capacity, boolean agentsToStuck) {		
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
			else if (l.getId().equals(Id.createLinkId("0_1")) || l.getId().equals(Id.createLinkId("1_2"))
					|| l.getId().equals(Id.createLinkId("5_6"))){	
				l.setCapacity(capacity);
				l.setFreespeed(200/(TTPerLink));
			} else {
				l.setCapacity(99999);
				l.setFreespeed(200/(TTPerLink));
			}
		}
	}
	
	private void createPopulation(Scenario scenario, TtCreateBraessPopulation.InitRoutes initRoutes) {	
		
		TtCreateBraessPopulation popCreator = new TtCreateBraessPopulation(
				scenario.getPopulation(), scenario.getNetwork());
		
		popCreator.setNumberOfPersons(personCount);
		
		// initialize all agents with only one route (the middle route) and no
		// initial score
		popCreator.createPersons(initRoutes, null);
	}

	/*
	 * Changes the BraessPopulation retrospectively(population has to be existent) 
	 * into a Population with the same start times.
	 */
	private void changePopulationToSameStartTime(Scenario scenario, double startTime) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = ((Activity)element);
					if (activity.getLinkId().toString().equals("0_1")) {
						activity.setEndTime(startTime);
					}
				}
			}
		}
	}

}
