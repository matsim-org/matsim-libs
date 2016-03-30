package playground.dhosse.av;

import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.vehicles.*;

public class AutonomousTaxiLowMaxVelocityExample {
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.loadConfig("/home/dhosse/at/config.xml",
				new TaxiConfigGroup());

		config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("/home/dhosse/at/output/");
        
        //Daniel, all vehicles will be of this type. Was this your intention? michalm, mar'16
        VehicleType type = VehicleUtils.getDefaultVehicleType();
        type.setMaximumVelocity(30 / 3.6);
		
		RunTaxiScenario.createControler(config, false).run();
	}
}
