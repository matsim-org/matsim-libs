package playground.balac.test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;



public class TestControler {
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final Config config = ConfigUtils.loadConfig(args[0]);    	
    	
		final Scenario sc = ScenarioUtils.loadScenario(config);		
		
		final Controler controler = new Controler( sc );
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		TestScoringFunctionFactory testScoringFunctionFactory = new TestScoringFunctionFactory(
				config,
				sc.getNetwork(), sc);
		controler.setScoringFunctionFactory(testScoringFunctionFactory);
		controler.run();
    	
	}

}
