/**
 * 
 */
package playground.kai.usecases.avchallenge;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
class KNAvChallenge {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig() ;
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
        final VrpDataImpl taxiData = new VrpDataImpl();
        new VehicleReader(scenario.getNetwork(), taxiData).readFile("filename");
        
        // ---

		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule(new TaxiModule( taxiData ) ) ;
		
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				this.bind( VrpOptimizer.class ).toInstance( new KNVrpOptimizer() ) ;
			}
		}) ;
		
		// ---

		controler.run();
	}

}
