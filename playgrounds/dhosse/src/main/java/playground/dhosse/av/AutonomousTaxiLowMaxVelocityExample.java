package playground.dhosse.av;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.vehicles.*;

import com.google.inject.name.Names;

public class AutonomousTaxiLowMaxVelocityExample {
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.loadConfig("/home/dhosse/at/config.xml",
				new TaxiConfigGroup());

		config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("/home/dhosse/at/output/");
        
        VehicleType avTaxiType = VehicleUtils.getFactory().createVehicleType(Id.create("avTaxiType", VehicleType.class));
        avTaxiType.setMaximumVelocity(30 / 3.6);
		
		Controler controler = RunTaxiScenario.createControler(config, false);
		controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install()
            {
                bind(VehicleType.class).annotatedWith(Names.named(TaxiModule.TAXI_MODE))
                .toInstance(avTaxiType);
            }
        });
		controler.run();
	}
}
