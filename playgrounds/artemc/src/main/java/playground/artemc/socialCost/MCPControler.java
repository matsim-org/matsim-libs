package playground.artemc.socialCost;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.scoring.DisaggregatedCharyparNagelScoringFunctionFactory;
import playground.artemc.scoring.DisaggregatedScoreAnalyzer;
import playground.artemc.socialCost.SocialCostController.Initializer;


public class MCPControler {

private static final Logger log = Logger.getLogger(MCPControler.class);
	
	static String configFile;
	static double sccBlendFactor;
	
	private static String input;
	private static String output;
	
	public static void main(String[] args) {
			
		input = args[0];
		output = args[1];
		sccBlendFactor = Double.parseDouble(args[2]);
		
		MCPControler runner = new MCPControler();
		runner.runInternalizationFlows();
	}
	
	private void runInternalizationFlows() {
		
		Controler controler = null;
		Scenario scenario = initSampleScenario();
		controler = new Controler(scenario);
	
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
	

	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig(input+"config.xml");
		config.controler().setOutputDirectory(output);
		config.network().setInputFile(input+"network.xml");
		config.plans().setInputFile(input+"population.xml.gz");
		config.transit().setTransitScheduleFile(input+"transitSchedule.xml");
		config.transit().setVehiclesFile(input+"vehicles.xml");

		//config.controler().setLastIteration(10);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}


}
