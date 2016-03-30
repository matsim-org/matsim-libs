package playground.wrashid.parkingChoice.priceoptimization;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.wrashid.parkingChoice.config.ParkingChoiceConfigGroup;

public class ParkingOptimizationControler {

public static void main(final String[] args) throws IOException {
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	
    	ParkingChoiceConfigGroup configGroupP = new ParkingChoiceConfigGroup();
    	config.addModule(configGroupP);
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final Controler controler = new Controler( sc );
				  
		    final OptimizationParkingModuleZH parkingModule = new OptimizationParkingModuleZH(controler);
		    
		    controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
			
					bind(OptimizationParkingModuleZH.class).toInstance(parkingModule);
					
				}
		    }
		    );
		   	
		    controler.run();
		}		
		
 
	

}
