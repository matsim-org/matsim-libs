package playground.wrashid.kti;

import kticompatibility.KtiPtConfigGroup;
import kticompatibility.KtiTripRouterFactory;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricing;

import playground.meisterk.kti.controler.KTIControler;

public class KTIScenarioWithRoadPricing {
	
	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		final String configFile = args[ 0 ];

		// read the config with our special parameters
		// Note that you need 
		final Config config = ConfigUtils.createConfig();
		config.addModule( new KtiPtConfigGroup() );
		ConfigUtils.loadConfig( config , configFile );

		// just make sure the scenario is loaded
		// Controler accepts a config, but if the Scenario is not
		// fully loaded when creating the routing module, we may get into
		// troubles later...
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		final Controler controler = new Controler( scenario );
		controler.setTripRouterFactory(
				new KtiTripRouterFactory(
					controler ) );
		controler.addControlerListener(new RoadPricing());

		// we're done!
		controler.run();
	}
	
}
