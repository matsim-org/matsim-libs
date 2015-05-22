package playground.balac.test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;



public class TestControler extends Controler {
	
	
	public TestControler(Scenario sc) {
		super(sc);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final Config config = ConfigUtils.loadConfig(args[0]);    	
    	
		final Scenario sc = ScenarioUtils.loadScenario(config);		
		
		final TestControler controler = new TestControler( sc );
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.init(config, sc.getNetwork(), sc);
    	controler.run();		   	
    	
	}
//	@Override
//	protected void loadControlerListeners() {
//		
//
//		super.loadControlerListeners();
//	}	
	
	public void init(Config config, Network network, Scenario sc) {
		TestScoringFunctionFactory testScoringFunctionFactory = new TestScoringFunctionFactory(
			      config, 
			      network, sc);
  this.setScoringFunctionFactory(testScoringFunctionFactory); 	
		
		
	}


}
