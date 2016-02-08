package playground.tschlenther.CarSharing.Cottbus;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.runExample.RunCarsharing;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tschlenther.Cottbus.Demand.CreateDemand;


public class RunCarSharingCottbus {

	public static void main(String[] args){
		Config config = new CarSharingConfigCreator().createConfig();
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		RunCarsharing.installCarSharing(controler);
		controler.run();
	}

	
}
