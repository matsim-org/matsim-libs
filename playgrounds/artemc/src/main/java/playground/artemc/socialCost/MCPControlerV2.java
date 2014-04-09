package playground.artemc.socialCost;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.artemc.socialCost.SocialCostController.Initializer;
import playground.artemc.socialCost.SocialCostControllerV2.InitializerV2;


public class MCPControlerV2 {

private static final Logger log = Logger.getLogger(MCPControlerV2.class);
	
	static String configFile;
	
	public static void main(String[] args) {
		
		configFile = args[0];
		MCPControlerV2 runner = new MCPControlerV2();
		runner.runInternalizationFlows(configFile);
	}
	
	private void runInternalizationFlows(String configFile) {
		Controler controler = new Controler(configFile);
		InitializerV2 initializer = new InitializerV2();
		controler.addControlerListener(initializer);
		
		// Additional analysis
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
