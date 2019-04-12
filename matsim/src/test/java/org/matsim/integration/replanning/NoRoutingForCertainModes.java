package org.matsim.integration.replanning;

import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoRoutingForCertainModes {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test public void testA() {

		URL dir = ExamplesUtils.getTestScenarioURL( "equil" );
		URL url = IOUtils.newUrl( dir, "config.xml" );
		Config config = ConfigUtils.loadConfig( url ) ;

		config.controler().setLastIteration( 1 );
		config.controler().setOutputDirectory( utils.getOutputDirectory() );

		final String strangeMode = "abc";
		{
			PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams( strangeMode ) ;
			config.planCalcScore().addModeParams( params );
		}
		config.plansCalcRoute().setIgnoredModes( Collections.singleton( strangeMode ) );

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		StageActivityTypes abc = new StageActivityTypesImpl( TransportMode.car ) ;
		PopulationFactory pf = scenario.getPopulation().getFactory();;

		for( Person person : scenario.getPopulation().getPersons().values() ){
			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips( person.getSelectedPlan(), abc );
			for( TripStructureUtils.Trip trip : trips ){
				trip.getOriginActivity().setLinkId( null );
				trip.getDestinationActivity().setLinkId( null );

				// ----

				Link originLink = NetworkUtils.getNearestLink( scenario.getNetwork(), trip.getOriginActivity().getCoord() );
				Link destinationLink = NetworkUtils.getNearestLink( scenario.getNetwork(), trip.getDestinationActivity().getCoord() ) ;

				List<PlanElement> def = new ArrayList<>() ;
				Leg leg = pf.createLeg( strangeMode ) ;
				leg.setTravelTime( 100. );
				def.add( leg ) ;

				Route route = pf.getRouteFactories().createRoute( GenericRouteImpl.class, originLink.getId(), destinationLink.getId() ) ;
				route.setTravelTime( leg.getTravelTime() );
				route.setDistance( 1000. );
				leg.setRoute( route ) ;

				TripRouter.insertTrip( person.getSelectedPlan(), trip.getOriginActivity(), def, trip.getDestinationActivity() ) ;
			}
		}


		// ---

		Controler controler = new Controler( scenario ) ;

//		controler.addOverridingModule( new AbstractModule(){
//			@Override
//			public void install(){
//				this.addRoutingModuleBinding( strangeMode ).toInstance( new RoutingModule(){
//					@Override public List<? extends PlanElement> calcRoute( Facility fromFacility, Facility toFacility, double departureTime, Person person ){
//						List<PlanElement> def = new ArrayList<>() ;
//						Leg leg = pf.createLeg( strangeMode ) ;
//						def.add( leg ) ;
//
//						Route route = pf.getRouteFactories().createRoute( GenericRouteImpl.class, fromFacility.getLinkId(), toFacility.getLinkId() ) ;
//						leg.setRoute( route ) ;
//
//						return def ;
//					}
//
//					@Override public StageActivityTypes getStageActivityTypes(){
//						return abc ;
//					}
//				} );
//			}
//		} ) ;

		// ---

		controler.run() ;

	}

}
