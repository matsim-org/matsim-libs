package matsimConnector.run;

import matsimConnector.engine.CAMobsimFactory;
import matsimConnector.engine.CATripRouterFactory;
import matsimConnector.network.HybridNetworkBuilder;
import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;
import matsimConnector.utility.Constants;
import matsimConnector.visualizer.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import matsimConnector.visualizer.debugger.eventsbaseddebugger.InfoBox;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import pedCA.environment.grid.EnvironmentGrid;
import pedCA.output.AgentTracker;
import pedCA.output.FundamentalDiagramWriter;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV6;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

public class CASimulationRunner implements IterationStartsListener{

	//private Controler controller;
	private static EventBasedVisDebuggerEngine dbg;
	
	@SuppressWarnings("deprecation")
	public static void main(String [] args) {
		String inputPath= Constants.INPUT_PATH;
		if (args.length>0){
			 inputPath = Constants.FD_TEST_PATH+args[0]+"/input";
			 Constants.SIMULATION_ITERATIONS = 1;
			 Constants.CA_TEST_END_TIME = 1800.;
			 Constants.SIMULATION_DURATION = 2000.;
		}
		Config c = ConfigUtils.loadConfig(inputPath+"/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(c);
		CAScenario scenarioCA = new CAScenario(inputPath+"/CAScenario");
		HybridNetworkBuilder.buildNetwork(scenarioCA.getCAEnvironment(Id.create("0", CAEnvironment.class)), scenarioCA);
		scenarioCA.connect(scenario);
		
		//new NetworkWriter(scenario.getNetwork()).write(c.network().getInputFile());
		
		c.controler().setWriteEventsInterval(1);
		c.controler().setLastIteration(Constants.SIMULATION_ITERATIONS-1);
		c.qsim().setEndTime(Constants.SIMULATION_DURATION);
		
		Controler controller = new Controler(scenario);
		
		
		//////////////------------THIS IS FOR THE SYSTEM OPTIMUM SEARCH
		TollHandler tollHandler = new TollHandler(controller.getScenario());
		TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
		controller.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
		// Define the pricing approach and the congestion implementation.
		//		controler.addControlerListener(new AverageCostPricing( (ScenarioImpl) controler.getScenario(), tollHandler ));
		controller.addControlerListener(new MarginalCongestionPricingContolerListener(controller.getScenario(), tollHandler, new CongestionHandlerImplV6(controller.getEvents(), (ScenarioImpl) controller.getScenario())));
		//////////////------------
		
		
		controller.setOverwriteFiles(true);
		
		CATripRouterFactory tripRouterFactoryCA = new CATripRouterFactory(scenario);
		controller.setTripRouterFactory(tripRouterFactoryCA);
		
		CAMobsimFactory factoryCA = new CAMobsimFactory();
		controller.addMobsimFactory(Constants.CA_MOBSIM_MODE, factoryCA);
		
		if (args.length==0){
			dbg = new EventBasedVisDebuggerEngine(scenario);
			InfoBox iBox = new InfoBox(dbg, scenario);
			dbg.addAdditionalDrawer(iBox);
			controller.getEvents().addHandler(dbg);
			EnvironmentGrid environmentGrid = scenarioCA.getEnvironments().get(Id.create("0",CAEnvironment.class)).getContext().getEnvironmentGrid();
			controller.getEvents().addHandler(new AgentTracker(Constants.OUTPUT_PATH+"/agentTrajectories.txt",environmentGrid.getRows(),environmentGrid.getColumns()));
		}else{
			controller.getEvents().addHandler(new FundamentalDiagramWriter(Double.parseDouble(args[0]),scenario.getPopulation().getPersons().size(), Constants.FD_TEST_PATH+"fd_data.csv"));
		}
		
		CASimulationRunner runner = new CASimulationRunner();
		controller.addControlerListener(runner);
		controller.run();
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (dbg != null)
			dbg.startIteration(event.getIteration()); 
	}

}
