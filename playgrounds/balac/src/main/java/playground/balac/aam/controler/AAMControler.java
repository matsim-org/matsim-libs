package playground.balac.aam.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.aam.router.MovingPathwaysTripRouterFactory;
import playground.balac.aam.scoring.AAMScoringFunctionFactory;


public class AAMControler {
	Controler cc ;

	private AAMControler(Scenario scenario) {
//		super(scenario);
		// TODO Auto-generated constructor stub
		cc = new Controler( scenario ) ;
		throw new RuntimeException( Gbl.RETROFIT_CONTROLER ) ;
	}

	public void init(Config config, Network network, Scenario sc) {
		AAMScoringFunctionFactory aAMScoringFunctionFactory = new AAMScoringFunctionFactory(
				      config, 
				      network, sc);
	    cc.setScoringFunctionFactory(aAMScoringFunctionFactory); 	
				
		}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final Config config = ConfigUtils.loadConfig(args[0]);

		
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final AAMControler controler = new AAMControler(sc);
		
		controler.setTripRouterFactory( new MovingPathwaysTripRouterFactory( sc ) );
				
		controler.init(config, sc.getNetwork(), sc);		
			
		controler.run();
	}

}
