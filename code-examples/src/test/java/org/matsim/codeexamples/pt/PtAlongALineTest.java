package org.matsim.codeexamples.pt;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PtAlongALineTest{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testPtAlongALine() {

		Config config = ConfigUtils.createConfig() ;

		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setLastIteration( 0 );

		Scenario scenario = ScenarioUtils.createScenario( config );
		// don't load anything

		NetworkFactory nf = scenario.getNetwork().getFactory();
		PopulationFactory pf = scenario.getPopulation().getFactory();
		ActivityFacilitiesFactory ff = scenario.getActivityFacilities().getFactory();

		// Construct a network and facilities along a line:
		// 0 --(0-1)-- 1 --(2-1)-- 2 -- ...
		// with a facility of same ID attached to each link.  Have home towards the left, and then select a shop facility, with frozen epsilons.

		Node prevNode;
		{
			Node node = nf.createNode( Id.createNodeId( 0 ), new Coord( 0., 0. ) );
			scenario.getNetwork().addNode( node );
			prevNode = node;
		}
		for( int ii = 1 ; ii <= 1000 ; ii++ ){
			Node node = nf.createNode( Id.createNodeId( ii ), new Coord( ii * 100, 0. ) );
			scenario.getNetwork().addNode( node );
			// ---
			addLinkAndFacility( scenario, nf, ff, prevNode, node );
			addLinkAndFacility( scenario, nf, ff, node, prevNode );
			// ---
			prevNode = node;
		}
		// ===
		List<ActivityFacility> facilitiesAsList = new ArrayList<>( scenario.getActivityFacilities().getFacilities().values() ) ;
		final Id<ActivityFacility> activityFacilityId = facilitiesAsList.get( facilitiesAsList.size()-1 ).getId() ;
		for( int jj = 0 ; jj < 1000 ; jj++ ){
			Person person = pf.createPerson( Id.createPersonId( jj ) );
			{
				scenario.getPopulation().addPerson( person );
				Plan plan = pf.createPlan();
				person.addPlan( plan );

				// --- 1st location at randomly selected facility:
				int idx = MatsimRandom.getRandom().nextInt( facilitiesAsList.size() );;
				Id<ActivityFacility> homeFacilityId = facilitiesAsList.get( idx ).getId();;
				Activity home = pf.createActivityFromActivityFacilityId( "dummy", homeFacilityId );
				home.setEndTime( 7. * 3600. );
				plan.addActivity( home );
				{
					Leg leg = pf.createLeg( "pt" );
					leg.setDepartureTime( 7. * 3600. );
					leg.setTravelTime( 1800. );
					plan.addLeg( leg );
				}
				{
					Activity shop = pf.createActivityFromActivityFacilityId( "dummy", activityFacilityId );
					// shop.setMaximumDuration( 3600. ); // does not work for locachoice: time computation is not able to deal with it.  yyyy replace by
					// more central code. kai, mar'19
					shop.setEndTime( 10. * 3600 );
					plan.addActivity( shop );
				}
//				{
//					Leg leg = pf.createLeg( "car" );
//					leg.setDepartureTime( 10. * 3600. );
//					leg.setTravelTime( 1800. );
//					plan.addLeg( leg );
//				}
//				{
//					Activity home2 = pf.createActivityFromActivityFacilityId( "home", homeFacilityId );
//					PopulationUtils.copyFromTo( home, home2 );
//					plan.addActivity( home2 );
//				}
			}
		}

		Node node0 = scenario.getNetwork().getNodes().get( Id.createNodeId( 0 ) );
		Gbl.assertNotNull(node0);

		Node nodeLast = scenario.getNetwork().getNodes().get( Id.createNodeId( 1000 ) ) ;
		Gbl.assertNotNull( nodeLast );

		Link loopLink0 = nf.createLink( Id.createLinkId( "loopLink0" ), node0, node0 ) ;
		loopLink0.setLength( 100. );
		scenario.getNetwork().addLink( loopLink0 );

		Link loopLinkLast = nf.createLink( Id.createLinkId( "loopLinkLast" ), nodeLast, nodeLast ) ;
		loopLink0.setLength( 100. );
		scenario.getNetwork().addLink( loopLinkLast );

		Link longTransitLink = nf.createLink( Id.createLinkId( "longTransitLink" ), node0, nodeLast ) ;
		longTransitLink.setLength( CoordUtils.calcEuclideanDistance( node0.getCoord(), nodeLast.getCoord() ) );
		scenario.getNetwork().addLink( longTransitLink );

		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();;

		Id<TransitStopFacility> facilityId0 = Id.create("StopFac0", TransitStopFacility.class ) ;
		Coord coordinate0 = new Coord(0.,0.) ;
		TransitStopFacility stopFacility0 = sf.createTransitStopFacility( facilityId0, coordinate0, false ) ;
		schedule.addStopFacility( stopFacility0 );

		Id<TransitStopFacility> facilityId10000 = Id.create("StopFac10000", TransitStopFacility.class ) ;
		Coord coordinate10000 = new Coord(10000.,0.) ;
		TransitStopFacility stopFacility10000 = sf.createTransitStopFacility( facilityId10000, coordinate10000, false ) ;
		schedule.addStopFacility( stopFacility10000 );

		{
			NetworkRoute route = pf.getRouteFactories().createRoute( NetworkRoute.class, loopLink0.getId(), loopLinkLast.getId() ) ;

			List<TransitRouteStop> stops = new ArrayList<>() ;
			{
				TransitRouteStop stop = sf.createTransitRouteStop( stopFacility0, 0., 0. );
				stops.add( stop ) ;
			}
			{
				TransitRouteStop stop = sf.createTransitRouteStop( stopFacility10000, 0., 0. );
				stops.add( stop ) ;
			}

			TransitRoute transitRoute = sf.createTransitRoute( Id.create("route1",TransitRoute.class), route, stops, "bus" ) ;

			TransitLine line = sf.createTransitLine( Id.create("line1", TransitLine.class) ) ;
			line.addRoute( transitRoute );

			schedule.addTransitLine( line );
		}

		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll( schedule, scenario.getNetwork() );
		TransitScheduleValidator.printResult( result );

		System.exit(-1) ;

		Controler controler = new Controler( scenario ) ;



		controler.run() ;
	}

	private static void addLinkAndFacility( Scenario scenario, NetworkFactory nf, ActivityFacilitiesFactory ff, Node prevNode, Node node ){
		final String str = prevNode.getId() + "-" + node.getId();
		Link link = nf.createLink( Id.createLinkId( str ), prevNode, node ) ;
		Set<String> set = new HashSet<>() ;
		set.add("car" ) ;
		link.setAllowedModes( set ) ;
		link.setLength( CoordUtils.calcEuclideanDistance( prevNode.getCoord(), node.getCoord() ) );
		link.setCapacity( 3600. );
		link.setFreespeed( 50./3.6 );
		scenario.getNetwork().addLink( link );
		// ---
		ActivityFacility af = ff.createActivityFacility( Id.create( str, ActivityFacility.class ), link.getCoord(), link.getId() ) ;
		ActivityOption option = ff.createActivityOption( "shop" ) ;
		af.addActivityOption( option );
		scenario.getActivityFacilities().addActivityFacility( af );
	}


}
