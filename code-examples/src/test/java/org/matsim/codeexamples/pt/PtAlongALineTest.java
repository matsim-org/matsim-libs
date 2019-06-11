package org.matsim.codeexamples.pt;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
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
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

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

		config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).setTeleportedModeSpeed( 3. );

		config.qsim().setEndTime( 24.*3600. );
		
		config.transit().setUseTransit(true);

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
		final int lastNodeIdx = 1000;
		final double deltaX = 100.;
		for( int ii = 1 ; ii <= lastNodeIdx ; ii++ ){
			Node node = nf.createNode( Id.createNodeId( ii ), new Coord( ii * deltaX, 0. ) );
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
				if ( jj==0 ){
					home.setEndTime( 7. * 3600. ); // one agent one sec earlier so that for all others the initial acts are visible in VIA
				} else {
					home.setEndTime( 7. * 3600. + 1. );
				}
				plan.addActivity( home );
				{
					Leg leg = pf.createLeg( "pt" );
					leg.setDepartureTime( 7. * 3600. );
					leg.setTravelTime( 1800. );
					plan.addLeg( leg );
				}
				{
					Activity shop = pf.createActivityFromActivityFacilityId( "dummy", activityFacilityId );
					plan.addActivity( shop );
				}
			}
		}

		final double deltaY = 1000.;

		Node node0 = nf.createNode( Id.createNodeId("trNode0"), new Coord(0, deltaY ) );
		scenario.getNetwork().addNode(node0);
		// ---
		Node node1 = nf.createNode( Id.createNodeId("trNode1"), new Coord(deltaX, deltaY ) ) ;
		scenario.getNetwork().addNode( node1 ) ;
		final Id<Link> TR_LINK_0_1_ID = Id.createLinkId( "trLink0-1" );
		createAndAddTransitLink( scenario, node0, node1, TR_LINK_0_1_ID );
		// ---
		Node nodeMiddleLeft = nf.createNode( Id.createNodeId("trNodeMiddleLeft") , new Coord( 0.5*(lastNodeIdx-1)*deltaX , deltaY ) ) ;
		scenario.getNetwork().addNode( nodeMiddleLeft) ;
		final Id<Link> TR_LONG_LINK_LEFT_ID = Id.createLinkId( "trLinkLongLeft" );
		{
			createAndAddTransitLink( scenario, node1, nodeMiddleLeft, TR_LONG_LINK_LEFT_ID );
		}
		// ---
		Node nodeMiddleRight = nf.createNode( Id.createNodeId("trNodeMiddleRight") , new Coord( 0.5*(lastNodeIdx+1)*deltaX , deltaY ) ) ;
		scenario.getNetwork().addNode( nodeMiddleRight ) ;
		final Id<Link> TR_LINK_MIDDLE_ID = Id.createLinkId( "trLinkMiddle" );
		{
			createAndAddTransitLink( scenario, nodeMiddleLeft, nodeMiddleRight, TR_LINK_MIDDLE_ID );
		}
		// ---
		Node nodeLastm1 = nf.createNode( Id.createNodeId("trNodeLastm1") , new Coord( (lastNodeIdx-1)*deltaX , deltaY ) ) ;
		scenario.getNetwork().addNode( nodeLastm1) ;
		final Id<Link> TR_LONG_LINK_RIGHT_ID = Id.createLinkId( "trLinkLongRight" );
		{
			createAndAddTransitLink( scenario, nodeMiddleRight, nodeLastm1, TR_LONG_LINK_RIGHT_ID );
		}

		// ---
		Node nodeLast = nf.createNode(Id.createNodeId("trNodeLast"), new Coord(lastNodeIdx*deltaX, deltaY ) ) ;
		scenario.getNetwork().addNode(nodeLast);
		final Id<Link> TR_LINK_LASTM1_LAST_ID = Id.createLinkId( "trLinkLastm1-Last" );
		{
			createAndAddTransitLink( scenario, nodeLastm1, nodeLast, TR_LINK_LASTM1_LAST_ID );
		}


		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory tsf = schedule.getFactory();
		VehiclesFactory tvf = scenario.getTransitVehicles().getFactory();

		TransitStopFacility stopFacility0 = tsf.createTransitStopFacility( Id.create( "StopFac0", TransitStopFacility.class ),
			  new Coord( deltaX, deltaY ), false );
		stopFacility0.setLinkId( TR_LINK_0_1_ID );
		schedule.addStopFacility( stopFacility0 );

		TransitStopFacility stopFacility10000 = tsf.createTransitStopFacility(
			  Id.create( "StopFac10000", TransitStopFacility.class ), new Coord( (lastNodeIdx - 1) * deltaX, deltaY ), false );
		stopFacility10000.setLinkId( TR_LINK_LASTM1_LAST_ID );
		schedule.addStopFacility( stopFacility10000 );

		TransitStopFacility stopFacility5000 = tsf.createTransitStopFacility(
			  Id.create( "StopFac5000", TransitStopFacility.class ), new Coord( (lastNodeIdx - 1) * deltaX, deltaY ), false );
		stopFacility5000.setLinkId( TR_LINK_MIDDLE_ID );
		stopFacility5000.getAttributes().putAttribute( "drtAccessible", true );
		stopFacility5000.getAttributes().putAttribute( "walkAccessible", false );
		schedule.addStopFacility( stopFacility5000 );

		VehicleType busType = tvf.createVehicleType( Id.create( "bus", VehicleType.class ) );
		{
			VehicleCapacity capacity = tvf.createVehicleCapacity();
			capacity.setSeats( 100 );
			busType.setCapacity( capacity );
		}
		{
			busType.setMaximumVelocity( 100./3.6 );
		}
		scenario.getTransitVehicles().addVehicleType( busType );

		{
			List<Id<Link>> linkIds = new ArrayList<>() ;
			linkIds.add( TR_LONG_LINK_LEFT_ID ) ;
			linkIds.add( TR_LINK_MIDDLE_ID ) ;
			linkIds.add( TR_LONG_LINK_RIGHT_ID ) ;
			NetworkRoute route = createNetworkRoute( TR_LINK_0_1_ID, linkIds, TR_LINK_LASTM1_LAST_ID, pf );

			List<TransitRouteStop> stops = new ArrayList<>() ;
			{
				TransitRouteStop stop = tsf.createTransitRouteStop( stopFacility0, 0., 0. );
				stops.add( stop ) ;
			}
			{
				TransitRouteStop stop = tsf.createTransitRouteStop( stopFacility10000, 0., 0. );
				stops.add( stop ) ;
			}
			{
				TransitRoute transitRoute = tsf.createTransitRoute( Id.create( "route1", TransitRoute.class ), route, stops, "bus" );
				{
					for ( int ii=0 ; ii<100 ; ii++ ){
						String str = "tr_" + ii ;

						scenario.getTransitVehicles().addVehicle( tvf.createVehicle( Id.createVehicleId( str ), busType ) );

						Departure departure = tsf.createDeparture( Id.create( str, Departure.class ), 7. * 3600. + ii*300 ) ;
						departure.setVehicleId( Id.createVehicleId( str ) );
						transitRoute.addDeparture( departure );
					}
				}
				TransitLine line = tsf.createTransitLine( Id.create( "line1", TransitLine.class ) );
				line.addRoute( transitRoute );

				schedule.addTransitLine( line );
			}
		}
		
		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll( schedule, scenario.getNetwork() );
		TransitScheduleValidator.printResult( result );

//		System.exit(-1) ;

		Controler controler = new Controler( scenario ) ;



		controler.run() ;
	}

	private static void createAndAddTransitLink( Scenario scenario, Node node0, Node node1, Id<Link> TR_LINK_0_1_ID ){
		Link trLink = scenario.getNetwork().getFactory().createLink( TR_LINK_0_1_ID, node0, node1 );
		trLink.setFreespeed( 100. / 3.6 );
		trLink.setCapacity( 100000. );
		scenario.getNetwork().addLink( trLink );
	}

	private static NetworkRoute createNetworkRoute( Id<Link> TR_LINK_0_1_ID, List<Id<Link>> linkIds, Id<Link> TR_LINK_LASTM1_LAST_ID, PopulationFactory pf ){
		NetworkRoute route = pf.getRouteFactories().createRoute( NetworkRoute.class, TR_LINK_0_1_ID, TR_LINK_LASTM1_LAST_ID ) ;
		route.setLinkIds( TR_LINK_0_1_ID, linkIds, TR_LINK_LASTM1_LAST_ID ) ;
		return route;
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
