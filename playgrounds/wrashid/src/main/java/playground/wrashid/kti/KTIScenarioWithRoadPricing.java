package playground.wrashid.kti;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.roadpricing.RoadPricing;

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
					controler ) );
		controler.setScoringFunctionFactory(
				new KtiLikeActivitiesScoringFunctionFactory(
					new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ),
					(KtiLikeScoringConfigGroup) config.getModule( KtiLikeScoringConfigGroup.GROUP_NAME ),
					config.planCalcScore(),
					scenario) );

		if ( config.scenario().isUseRoadpricing() ) {
			log.info( "adding the roadpricing listenner." );
			controler.addControlerListener( new RoadPricing() );
		}
		else {
			log.info( "NOT adding the roadpricing listenner." );
		}

		// we're done!
		controler.run();
	}
}
