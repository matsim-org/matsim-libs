package playground.wrashid.kti;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import playground.ivt.kticompatibility.KtiLikeActivitiesScoringFunctionFactory;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.kticompatibility.KtiPtConfigGroup;
import playground.ivt.kticompatibility.KtiTripRouterFactory;

public class KTIScenarioWithRoadPricing {
	private static final Logger log =
		Logger.getLogger(KTIScenarioWithRoadPricing.class);

	
	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		final String configFile = args[ 0 ];

		// read the config with our special parameters
		// Note that you need scoring parameters converted
		// from the KTI config by something like
		// playground.thibautd.scripts.KtiToSimiliKtiConfig
		final Config config = ConfigUtils.createConfig();
		config.addModule( new KtiPtConfigGroup() );
		config.addModule( new KtiLikeScoringConfigGroup() );
		ConfigUtils.loadConfig( config , configFile );

		// just make sure the scenario is loaded
		// Controler accepts a config, but if the Scenario is not
		// fully loaded when creating the routing module, we may get into
		// troubles later...
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		final Controler controler = new Controler( scenario );
		controler.setTripRouterFactory(
				new KtiTripRouterFactory(
					scenario ) );
		controler.setScoringFunctionFactory(
				new KtiLikeActivitiesScoringFunctionFactory(
					new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ),
					(KtiLikeScoringConfigGroup) config.getModule( KtiLikeScoringConfigGroup.GROUP_NAME ),
						scenario) );

//        if (ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).isUsingRoadpricing()) {
			Logger.getLogger("KTIScenarioWithRoadPricing.class").fatal("this above syntax is no longer there.  "
					+ "If you really need to configure this via a config group, please construct your own config group for that purpose.  kai, sep'14");
		
			log.info( "adding the roadpricing listenner." );
        controler.setModules(new ControlerDefaultsWithRoadPricingModule());
        //		}
//		else {
//			log.info( "NOT adding the roadpricing listenner." );
//		}

		// we're done!
		controler.run();

		// finally, dump plans in V4 to allow hot start
		new PopulationWriter(
				scenario.getPopulation(),
				scenario.getNetwork() ).writeV4(
					controler.getControlerIO().getOutputFilename( "output_plans_v4.xml.gz" ) );
	}
}
