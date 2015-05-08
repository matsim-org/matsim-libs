package playground.balac.test;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;



public class TestControler {
	static Controler controler ;
	
	
//	public TestControler(Scenario sc) {
//		super(sc);
//		// TODO Auto-generated constructor stub
//	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getLogger("dummy").fatal( Gbl.RETROFIT_CONTROLER ) ;
		System.exit(-1) ;
		
		final Config config = ConfigUtils.loadConfig(args[0]);    	
    	
		final Scenario sc = ScenarioUtils.loadScenario(config);		
		
		controler = new Controler( sc );
	    controler.setOverwriteFiles(true);
	    init(config, sc.getNetwork(), sc);	
    	controler.run();		   	
    	
	}
//	@Override
//	protected void loadControlerListeners() {
//		
//
//		super.loadControlerListeners();
//	}	
	
	public static void init(Config config, Network network, Scenario sc) {
		TestScoringFunctionFactory testScoringFunctionFactory = new TestScoringFunctionFactory(
			      config, 
			      network, sc);
		controler.setScoringFunctionFactory(testScoringFunctionFactory); 	
		
		
	}


}
