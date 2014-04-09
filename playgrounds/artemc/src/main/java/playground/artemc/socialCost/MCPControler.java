package playground.artemc.socialCost;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.artemc.socialCost.SocialCostController.Initializer;


public class MCPControler {

private static final Logger log = Logger.getLogger(MCPControler.class);
	
	static String configFile;
	
	public static void main(String[] args) {
		
		configFile = args[0];
		MCPControler runner = new MCPControler();
		runner.runInternalizationFlows(configFile);
	}
	
	private void runInternalizationFlows(String configFile) {
		Controler controler = new Controler(configFile);
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);
		
		// Additional analysis
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
