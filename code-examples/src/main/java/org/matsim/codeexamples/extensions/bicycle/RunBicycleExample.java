package org.matsim.codeexamples.extensions.bicycle;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.Vehicle;

import java.util.Arrays;
import java.util.HashSet;

import static org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

public final class RunBicycleExample{

	public static void main ( String [] args ) {

		Config config ;
		if ( args != null && args.length>=1 ) {
			config = ConfigUtils.loadConfig( args ) ;
		} else {
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) ) ;
		}

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 1 );

		config.plansCalcRoute().setNetworkModes( Arrays.asList( TransportMode.car, TransportMode.bike ) );
		config.plansCalcRoute().removeTeleportedModeParams( TransportMode.bike );
		{
			StrategySettings stratSets = new StrategySettings();
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice );
			stratSets.setWeight( 1. );
			config.strategy().addStrategySettings( stratSets );

			config.subtourModeChoice().setModes( new String[]{ TransportMode.car, TransportMode.bike } ) ;
			config.subtourModeChoice().setChainBasedModes( new String[]{ TransportMode.car, TransportMode.bike } );
		}
		config.qsim().setMainModes( Arrays.asList( TransportMode.car, TransportMode.bike ) );

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config );

		for( Link link : scenario.getNetwork().getLinks().values() ){
			link.setAllowedModes( new HashSet<>( Arrays.asList( TransportMode.car, TransportMode.bike ) ) ) ;
		}

		// ---

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				this.addTravelTimeBinding( TransportMode.bike ).toInstance( new TravelTime(){
					@Inject @Named(TransportMode.bike) TravelTimeCalculator bikeCalculator ;
					@Override public double getLinkTravelTime( Link link, double time, Person person, Vehicle vehicle ){
						double maxSpeedFromLink = link.getFreespeed( time );

						double maxSpeedFromVehicle = 12.5/3.6 ;
						Object isExpressway = link.getAttributes().getAttribute( "isExpressway" );
						if ( isExpressway != null && (boolean) isExpressway ) {
							maxSpeedFromVehicle = 25./3.6 ;
						}

						double maxSpeedFromObservation = bikeCalculator.getLinkTravelTimes().getLinkTravelTime( link, time, person, vehicle ) ;

						double actualSpeed = Math.min( maxSpeedFromLink, Math.min( maxSpeedFromObservation, maxSpeedFromVehicle ) ) ;

						return link.getLength()/actualSpeed ;
					}
				} );
			}
		} ) ;

		// ---

		controler.run() ;

	}

}
