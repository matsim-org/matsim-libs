/**
 * 
 */
package playground.kai.usecases.avchallenge;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class KNAvChallenge {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig() ;
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
        final TaxiData taxiData = new TaxiData();
        new VehicleReader(scenario.getNetwork(), taxiData).readFile("filename");
        
        // ---

		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule(new TaxiModule( taxiData ) ) ;
		
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				this.bind( VrpOptimizer.class ).toInstance( new MyVrpOptimizer() ) ;
			}
		}) ;
		
		// ---

		controler.run();
	}

}
