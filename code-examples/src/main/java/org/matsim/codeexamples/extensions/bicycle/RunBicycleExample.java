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
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.Vehicle;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class RunBicycleExample{

	public static void main ( String [] args ) {

//		Config config ;
//		if ( args != null && args.length>=1 ) {
//			config = ConfigUtils.loadConfig( args ) ;
//		} else {
//			config =
//				  ConfigUtils.loadConfig( IOUtils.newUrl( ExamplesUtils.getTestScenarioURL( "equil-mixedTraffic" ), "config-with-mode-vehicles.xml" ) ) ;
//		}

		Config config = ConfigUtils.createConfig() ;


		config.plansCalcRoute().setNetworkModes( Arrays.asList( TransportMode.car, TransportMode.bike ) );

		config.plansCalcRoute().removeModeRoutingParams( TransportMode.bike );

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		config.qsim().setMainModes( Arrays.asList( TransportMode.car, TransportMode.bike ) );

		config.travelTimeCalculator().setSeparateModes( true );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				this.addTravelTimeBinding( TransportMode.bike ).toInstance( new TravelTime(){
					@Inject @Named(TransportMode.bike) TravelTimeCalculator bikeCalculator ;
//					@Inject Set<TravelTimeCalculator> ttimeCalcSet ;
					@Inject Map<String,TravelTime> ttimeMap ;
					@Override public double getLinkTravelTime( Link link, double time, Person person, Vehicle vehicle ){
						double maxSpeedFromLink = link.getFreespeed( time );

						double maxSpeedFromVehicle = 15./3.6 ;

						double maxSpeedFromObservation = 0. ;

						double actualSpeed = Math.min( maxSpeedFromLink, Math.min( maxSpeedFromObservation, maxSpeedFromVehicle ) ) ;
						return link.getLength()/actualSpeed ;
					}
				} );
			}
		} ) ;

		controler.run() ;

	}

}
