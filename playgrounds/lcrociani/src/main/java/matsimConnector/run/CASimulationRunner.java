package matsimConnector.run;

import matsimConnector.engine.CAMobsimFactory;
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
import org.matsim.core.scenario.ScenarioUtils;

import pedCA.output.OutputManager;

public class CASimulationRunner implements IterationStartsListener{

	private Controler controller;
	
	public static void main(String [] args) {
		String inputPath= Constants.INPUT_PATH;
		String outputPath = Constants.OUTPUT_PATH;
		if (args.length>0){
			 inputPath = Constants.FD_TEST_PATH+args[0]+"/input";
			 outputPath = Constants.FD_TEST_PATH+args[0]+"/output";
		}
		Config c = ConfigUtils.loadConfig(inputPath+"/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(c);
		CAScenario scenarioCA = new CAScenario(inputPath+"/CAScenario");
		HybridNetworkBuilder.buildNetwork(scenarioCA.getCAEnvironment(Id.create("0", CAEnvironment.class)), scenarioCA);
		scenarioCA.connect(scenario);
		
		//new NetworkWriter(scenario.getNetwork()).write(c.network().getInputFile());
		
		c.controler().setWriteEventsInterval(1);
		c.controler().setLastIteration(0);
		c.qsim().setEndTime(Constants.CA_TEST_END_TIME+100);
		
		Controler controller = new Controler(scenario);
		try{
			org.matsim.core.utils.io.IOUtils.deleteDirectory(new java.io.File(outputPath));
		}catch(IllegalArgumentException e){
			
		}
		
		CAMobsimFactory factoryCA = new CAMobsimFactory();
		controller.addMobsimFactory(Constants.CA_MOBSIM_MODE, factoryCA);
		
		if (args.length==0){
		EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(scenario);
		InfoBox iBox = new InfoBox(dbg, scenario);
		dbg.addAdditionalDrawer(iBox);
		controller.getEvents().addHandler(dbg);
		}else{
			controller.getEvents().addHandler(new OutputManager(Double.parseDouble(args[0]),scenario.getPopulation().getPersons().size()));
		}
		
		controller.run();
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if ((event.getIteration()) % 1 == 0 || event.getIteration() > 50) {
//			this.factory.debug(this.visDebugger);
			//this.controller.getEvents().addHandler(this.qSimDrawer);
			this.controller.setCreateGraphs(true);
		} else {
//			this.factory.debug(null);
			//this.controller.getEvents().removeHandler(this.qSimDrawer);
			this.controller.setCreateGraphs(false);
		}
//		this.visDebugger.setIteration(event.getIteration());
		
	}

}
