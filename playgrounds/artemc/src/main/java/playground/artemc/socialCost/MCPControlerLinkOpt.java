package playground.artemc.socialCost;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.heterogeneity.scoring.HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory;
import playground.artemc.heterogeneity.scoring.DisaggregatedHeterogeneousScoreAnalyzer;
import playground.artemc.socialCost.SocialCostControlerLinkOpt.InitializerLinkOpt;


public class MCPControlerLinkOpt{

private static final Logger log = Logger.getLogger(MCPControler.class);
	
	static String configFile;
	static double sccBlendFactor;
	
	private static String input;
	private static String output;
	
	public static void main(String[] args) {
			
		input = args[0];
		output = args[1];
		sccBlendFactor = Double.parseDouble(args[2]);
		
		MCPControlerLinkOpt runner = new MCPControlerLinkOpt();
		runner.runInternalizationFlows();
	}
	
	private void runInternalizationFlows() {
		
		Controler controler = null;
		Scenario scenario = initSampleScenario();
		controler = new Controler(scenario);
	
		InitializerLinkOpt initializer = new InitializerLinkOpt(sccBlendFactor);
		controler.addControlerListener(initializer);
		
		// Additional analysis
		
		ScenarioImpl scnearioImpl = (ScenarioImpl) controler.getScenario();
        controler.setScoringFunctionFactory(new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork()));
		controler.addControlerListener(new SimpleAnnealer());
		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((ScenarioImpl) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);
		controler.addControlerListener(new DisaggregatedHeterogeneousScoreAnalyzer((ScenarioImpl) controler.getScenario(),analysisControlerListener.getTripAnalysisHandler()));
		controler.run();
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
		config.controler().setOutputDirectory(output);
		
		//		config.controler().setLastIteration(10);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}


}
