package org.matsim.codeexamples.integration;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;
import java.util.*;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode;

final class RunMultipleModesExample{



	private static final Logger log = Logger.getLogger(RunMultipleModesExample.class) ;

	private Config config;

	public static void main( String [] args ) {
		RunMultipleModesExample abc = new RunMultipleModesExample();
		abc.run() ;
	}

	public final void run() {
		if ( config==null ) {
			prepareConfig() ;
		}

		Scenario scenario = ScenarioUtils.loadScenario( config );

		// add the modes to each link:
		for( Link link : scenario.getNetwork().getLinks().values() ){
			Set<String> modes = new LinkedHashSet<>( Arrays.asList( TransportMode.car, TransportMode.bike ));
			link.setAllowedModes( modes );
		}

		Controler controler = new Controler( scenario ) ;
		controler.run() ;
	}

	final Config prepareConfig(){
		final URL url = IOUtils.newUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
		log.warn("url=" + url.toString() ) ;
		config = ConfigUtils.loadConfig( url ) ;

		{ // add strategy that switches between car and bike:
			StrategySettings stratSets = new StrategySettings(  ) ;
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice );
			stratSets.setWeight( 1. );
			config.strategy().addStrategySettings( stratSets );

//			config.changeMode().setModes( new String [] { TransportMode.car, TransportMode.bike} );
			config.subtourModeChoice().setModes(  new String [] { TransportMode.car, TransportMode.bike} );
			config.subtourModeChoice().setChainBasedModes( new String [] { TransportMode.car, TransportMode.bike}  );
		}
		{ // configure the bike mode:

			// add it to the list of network routing modes:
			config.plansCalcRoute().setNetworkModes( Arrays.asList( TransportMode.car, TransportMode.bike ) );

			// one also needs to remove the default teleportation bike router:
			config.plansCalcRoute().removeModeRoutingParams( TransportMode.bike );

			// say that the the travel times need to be analyzed also for the bike mode:
//			config.travelTimeCalculator().setAnalyzedModes( new LinkedHashSet<>( Arrays.asList( TransportMode.bike, TransportMode.car ) ) ) ;

			// set up bike scoring:
			ModeParams params = new ModeParams( TransportMode.bike ) ;
			config.planCalcScore().addModeParams( params );

			// set up the bike qsim
			config.qsim().setMainModes( new HashSet<>( Arrays.asList( TransportMode.car, TransportMode.bike ) ) ) ;
		}

		config.travelTimeCalculator().setSeparateModes( true ); // otherwise, router will use speeds averaged over modes.
//		config.travelTimeCalculator().setSeparateModes( false ); // this used to be the default

//		config.plansCalcRoute().setInsertingAccessEgressWalk( true );

		return config ;
	}


}
