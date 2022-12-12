package org.matsim.codeexamples.integration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.Vehicle;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;

final class RunMultipleModesExample{

	private static final Logger log = LogManager.getLogger(RunMultipleModesExample.class) ;

	public static void main( String [] args ) {
		Config config = prepareConfig() ;

		Scenario scenario = prepareScenario( config );

		Controler controler = prepareControler( scenario );

		controler.run() ;
	}

	static Controler prepareControler( Scenario scenario ){
		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				this.addTravelTimeBinding( TransportMode.bike ).to( BikeTravelTime.class ) ;
			}
		} ) ;
		return controler;
	}

	static Scenario prepareScenario( Config config ){
		Scenario scenario = ScenarioUtils.loadScenario( config );

		// add the modes to each link:
		for( Link link : scenario.getNetwork().getLinks().values() ){
			Set<String> modes = new LinkedHashSet<>( Arrays.asList( TransportMode.car, TransportMode.bike ));
			link.setAllowedModes( modes );
		}
		return scenario;
	}

	static Config prepareConfig(){
		final URL url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
		log.warn("url=" + url) ;
		Config config = ConfigUtils.loadConfig( url );

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
			config.plansCalcRoute().removeTeleportedModeParams( TransportMode.bike );

			// say that the the travel times need to be analyzed also for the bike mode:
//			config.travelTimeCalculator().setAnalyzedModes( new LinkedHashSet<>( Arrays.asList( TransportMode.bike, TransportMode.car ) ) ) ;
			// no longer needed; by default all network modes are analyzed

			// set up bike scoring:
			ModeParams params = new ModeParams( TransportMode.bike ) ;
			config.planCalcScore().addModeParams( params );

			// set up the bike qsim
			config.qsim().setMainModes( new HashSet<>( Arrays.asList( TransportMode.car, TransportMode.bike ) ) ) ;
		}

//		config.travelTimeCalculator().setSeparateModes( true ); // otherwise, router will use speeds averaged over modes.  For 11.x, this is the default.
//		config.travelTimeCalculator().setSeparateModes( false ); // this used to be the default

//		config.plansCalcRoute().setInsertingAccessEgressWalk( true );

		return config ;
	}


	private static class BikeTravelTime implements TravelTime{
		@Override public double getLinkTravelTime( Link link, double time, Person person, Vehicle vehicle ){
			return 1. ;
		}
	}
}
