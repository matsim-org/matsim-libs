package playground.artemc.socialCost;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.scoreAnalyzer.DisaggregatedCharyparNagelScoringFunctionFactory;
import playground.artemc.scoreAnalyzer.DisaggregatedScoreAnalyzer;
import playground.artemc.socialCost.SocialCostController.Initializer;


public class MCPControler {

private static final Logger log = Logger.getLogger(MCPControler.class);
	
	static String configFile;
	static double sccBlendFactor;
	
	public static void main(String[] args) {
		
		configFile = args[0];
		sccBlendFactor = Double.parseDouble(args[1]);
		
		MCPControler runner = new MCPControler();
		runner.runInternalizationFlows(configFile);
	}
	
	private void runInternalizationFlows(String configFile) {
		Controler controler = new Controler(configFile);
		Initializer initializer = new Initializer(sccBlendFactor);
		controler.addControlerListener(initializer);
		
		// Additional analysis
		
		ScenarioImpl scnearioImpl = (ScenarioImpl) controler.getScenario();
		controler.setScoringFunctionFactory(new DisaggregatedCharyparNagelScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getNetwork()));
		controler.addControlerListener(new DisaggregatedScoreAnalyzer(scnearioImpl));
		controler.addControlerListener(new SimpleAnnealer());
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
