package matsimConnector.run;

import matsimConnector.engine.CAMobsimFactory;
import matsimConnector.network.HybridNetworkBuilder;
import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;
import matsimConnector.utility.Constants;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;

public class CASimulationRunner implements IterationStartsListener{

	private Controler controller;
	
	public static void main(String [] args) {
		String inputPath = Constants.INPUT_PATH;
		Config c = ConfigUtils.loadConfig(inputPath+"/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(c);
		CAScenario scenarioCA = new CAScenario(inputPath+"/CAScenario");
		HybridNetworkBuilder.buildNetwork(scenarioCA.getCAEnvironment(Id.create("0", CAEnvironment.class)), scenarioCA);
		scenarioCA.connect(scenario);
		
		//new NetworkWriter(scenario.getNetwork()).write(c.network().getInputFile());
		
		
		c.controler().setWriteEventsInterval(1);
		c.controler().setLastIteration(0);
		c.qsim().setEndTime(60);
		
		Controler controller = new Controler(scenario);
		//controller.setOverwriteFiles(true);
		
		CAMobsimFactory factoryCA = new CAMobsimFactory();
		controller.addMobsimFactory(Constants.CA_MOBSIM_MODE, factoryCA);
		
		//if (args.length > 0 && args[0].equals("true")) {
			// VIS only

			//EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(scenario);
			//InfoBox iBox = new InfoBox(dbg, scenario);
			//dbg.addAdditionalDrawer(iBox);
			//controller.getEvents().addHandler(dbg);
			//dbg.addAdditionalDrawer(new Branding());
			//QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
			//dbg.addAdditionalDrawer(qDbg);
			//controller.getEvents().addHandler(qDbg);
		//}
		
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
