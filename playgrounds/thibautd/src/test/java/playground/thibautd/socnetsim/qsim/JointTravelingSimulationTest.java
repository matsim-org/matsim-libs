/* *********************************************************************** *
 * project: org.matsim.*
 * JointTravelingSimulationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.socnetsim.qsim;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class JointTravelingSimulationTest {
	private static final Logger log =
		Logger.getLogger(JointTravelingSimulationTest.class);

	private static final int N_LAPS = 5;

	private static final Id ORIGIN_LINK = new IdImpl( "origin" );
	private static final Id TO_PU_LINK = new IdImpl( "toPu" );
	private static final Id PU_LINK = new IdImpl( "pu" );
	private static final Id TRAVEL_LINK_1 = new IdImpl( 1 );
	private static final Id TRAVEL_LINK_2 = new IdImpl( 2 );
	private static final Id DO_LINK = new IdImpl( "do" );
	private static final Id TO_DESTINATION_LINK = new IdImpl( "to_destination" );
	private static final Id DESTINATION_LINK = new IdImpl( "destination" );
	private static final Id RETURN_LINK = new IdImpl( "return" );
	
	@Test
	public void testAgentsArriveTogetherWithoutDummies() throws Exception {
		testAgentsArriveTogether( false , false );
	}

	@Test
	public void testAgentsArriveTogetherWithDummies() throws Exception {
		testAgentsArriveTogether( true , false );
	}

	@Test
	public void testAgentsArriveTogetherWithDummiesAndDoAtPu() throws Exception {
		testAgentsArriveTogether( true , true );
	}

	@Test
	public void testAgentsArriveTogetherWithoutDummiesAndDoAtPu() throws Exception {
		testAgentsArriveTogether( false , true );
	}

	public void testAgentsArriveTogether(final boolean insertDummies, final boolean dropOffAtPu) throws Exception {
		final Random random = new Random( 1234 );

		for (int i=0; i < 50; i++) {
			log.info( "random test scenario "+i );
			final Scenario sc = dropOffAtPu ?
				createTestScenarioWithDoAtPu( insertDummies , random ) :
				createTestScenario( insertDummies , random );
			final EventsManager events = EventsUtils.createEventsManager();

			final AtomicInteger arrCount = new AtomicInteger( 0 );
			final AtomicInteger atDestCount = new AtomicInteger( 0 );
			events.addHandler( new PersonArrivalEventHandler() {
				private double arrival = -100;
				private int nArrival = 0;

				@Override
				public void reset(final int iteration) {}

				@Override
				public void handleEvent(final PersonArrivalEvent event) {
					final String mode = event.getLegMode();
					log.info( mode+" arrival at "+event.getTime() );
					if ( mode.equals( JointActingTypes.DRIVER ) ||
							mode.equals( JointActingTypes.PASSENGER ) ) {
						if ( nArrival++ % 3 == 0 ) {
							arrival = event.getTime();
							assert arrival > 0;
						}

						arrCount.incrementAndGet();
						Assert.assertEquals(
							"unexpected joint arrival time",
							arrival,
							event.getTime(),
							MatsimTestUtils.EPSILON);

						Assert.assertEquals(
								"unexpected arrival location for mode "+mode,
								dropOffAtPu ? PU_LINK : DO_LINK,
								event.getLinkId() );
					}

					if ( event.getLinkId().equals( DESTINATION_LINK ) ) {
						atDestCount.incrementAndGet();
					}
				}
			});

			final JointQSimFactory factory = new JointQSimFactory( );
			factory.createMobsim( sc , events ).run();

			Assert.assertEquals(
					"unexpected number of joint arrivals",
					N_LAPS * 3,
					arrCount.get());

			Assert.assertEquals(
					"unexpected number of agents arriving at destination",
					N_LAPS * sc.getPopulation().getPersons().size(),
					atDestCount.get());
		}
	}

	@Test
	public void testNumberOfEnterLeaveVehicle() throws Exception {
		final Random random = new Random( 1234 );

		for (int i=0; i < 50; i++) {
			log.info( "random test scenario "+i );
			final Scenario sc = createTestScenario( false , random );
			final EventsManager events = EventsUtils.createEventsManager();

			final AtomicInteger enterCount = new AtomicInteger( 0 );
			final AtomicInteger leaveCount = new AtomicInteger( 0 );
			events.addHandler( new PersonEntersVehicleEventHandler() {
				@Override
				public void reset(int iteration) {}

				@Override
				public void handleEvent(final PersonEntersVehicleEvent event) {
					enterCount.incrementAndGet();
				}
			});

			events.addHandler( new PersonLeavesVehicleEventHandler() {
				@Override
				public void reset(int iteration) {}

				@Override
				public void handleEvent(final PersonLeavesVehicleEvent event) {
					leaveCount.incrementAndGet();
				}
			});

			final JointQSimFactory factory = new JointQSimFactory( );
			factory.createMobsim( sc , events ).run();

			Assert.assertEquals(
					"not as many leave events as enter events",
					enterCount.get(),
					leaveCount.get());
		}
	}

	private static Scenario createTestScenario(
			final boolean insertDummyActivities,
			final Random random) {
		return createTestScenario(
				insertDummyActivities,
				PU_LINK,
				DO_LINK,
				Arrays.asList( TO_PU_LINK ),
				Arrays.asList( TRAVEL_LINK_1 , TRAVEL_LINK_2 ),
				Arrays.asList( TO_DESTINATION_LINK ),
				random);
	}

	private static Scenario createTestScenarioWithDoAtPu(
			final boolean insertDummyActivities,
			final Random random) {
		return createTestScenario(
				insertDummyActivities,
				PU_LINK,
				PU_LINK,
				Arrays.asList( TO_PU_LINK ),
				Collections.<Id> emptyList(),
				Arrays.asList( TRAVEL_LINK_1 , TRAVEL_LINK_2 , TO_DESTINATION_LINK ),
				random);
	}

	private static Scenario createTestScenario(
			final boolean insertDummyActivities,
			final Id puLink,
			final Id doLink,
			final List<Id> toPuRoute,
			final List<Id> puToDoRoute,
			final List<Id> doToDestRoute,
			final Random random) {
		final Config config = ConfigUtils.createConfig();
		final QSimConfigGroup qsimConf = config.qsim();
		qsimConf.setEndTime( 30 * 3600 );
		final Scenario sc = ScenarioUtils.createScenario( config );

		createNetwork( sc.getNetwork() );

		final Id driverId = new IdImpl( "driver" );
		final Id passengerId1 = new IdImpl( "passenger_1" );
		final Id passengerId2 = new IdImpl( "passenger_2" );

		final PopulationFactory factory = sc.getPopulation().getFactory();

		// TODO: complicate:
		// - same OD served several times by one driver for different passengers
		// - different drivers serve the same OD

		/* driver */ {
			final Person driver = factory.createPerson( driverId );
			final Plan driverPlan = factory.createPlan();
			driverPlan.setPerson( driver );
			driver.addPlan( driverPlan );
			sc.getPopulation().addPerson( driver );

			for ( int i=0; i < N_LAPS; i++ ) {
				Activity act = factory.createActivityFromLinkId( "h" , ORIGIN_LINK );
				act.setEndTime( (i * 12) + random.nextDouble() * 6 * 3600 );
				driverPlan.addActivity( act );

				Leg l = factory.createLeg( TransportMode.car );
				l.setRoute( new LinkNetworkRouteImpl( ORIGIN_LINK , toPuRoute , puLink ) );
				driverPlan.addLeg( l );

				if (insertDummyActivities) {
					act = factory.createActivityFromLinkId(
								JointActingTypes.INTERACTION,
								puLink );
					driverPlan.addActivity( act );
					act.setMaximumDuration( 0 );
				}

				l = factory.createLeg( JointActingTypes.DRIVER );
				DriverRoute dRoute =
					new DriverRoute(
							puLink,
							doLink );
				dRoute.setLinkIds(
						puLink , 
						puToDoRoute,
						doLink);
				dRoute.addPassenger( passengerId1 );
				dRoute.addPassenger( passengerId2 );
				l.setRoute( dRoute );
				driverPlan.addLeg( l );

				if (insertDummyActivities) {
					act = factory.createActivityFromLinkId(
								JointActingTypes.INTERACTION,
								doLink );
					driverPlan.addActivity( act );
					act.setMaximumDuration( 0 );
				}

				l = factory.createLeg( TransportMode.car );
				l.setRoute( new LinkNetworkRouteImpl( doLink , doToDestRoute , DESTINATION_LINK ) );
				driverPlan.addLeg( l );

				final Activity work = factory.createActivityFromLinkId( "w" , DESTINATION_LINK );
				work.setEndTime( (i * 12) + random.nextDouble() * 12 * 3600 );
				driverPlan.addActivity( work );

				l = factory.createLeg( TransportMode.car );
				l.setRoute(
						new LinkNetworkRouteImpl(
							DESTINATION_LINK,
							Collections.singletonList( RETURN_LINK ),
							ORIGIN_LINK ) );
				driverPlan.addLeg( l );
			}

			final Activity act = factory.createActivityFromLinkId( "h" , ORIGIN_LINK );
			driverPlan.addActivity( act );
		}

		// passengers 
		for (Id passengerId : new Id[]{ passengerId1 , passengerId2 }) {
			final Person p1 = factory.createPerson( passengerId );
			final Plan p1Plan = factory.createPlan();
			p1Plan.setPerson( p1 );
			p1.addPlan( p1Plan );
			sc.getPopulation().addPerson( p1 );

			for ( int i=0; i < N_LAPS; i++ ) {
				Activity act = factory.createActivityFromLinkId( "h" , ORIGIN_LINK );
				act.setEndTime( (i * 12) + random.nextDouble() * 6 * 3600 );
				p1Plan.addActivity( act );

				Leg l = factory.createLeg( TransportMode.walk );
				double tt = random.nextDouble() * 1234;
				Route walkRoute = new GenericRouteImpl( ORIGIN_LINK , puLink );
				walkRoute.setTravelTime( tt );
				l.setTravelTime( tt );
				l.setRoute( walkRoute );
				p1Plan.addLeg( l );

				if (insertDummyActivities) {
					act = factory.createActivityFromLinkId(
								JointActingTypes.INTERACTION,
								puLink );
					p1Plan.addActivity( act );
					act.setMaximumDuration( 0 );
				}

				l = factory.createLeg( JointActingTypes.PASSENGER );
				PassengerRoute pRoute =
					new PassengerRoute(
							puLink,
							doLink );
				pRoute.setDriverId( driverId );
				l.setRoute( pRoute );
				p1Plan.addLeg( l );

				if (insertDummyActivities) {
					act = factory.createActivityFromLinkId(
								JointActingTypes.INTERACTION,
								doLink );
					p1Plan.addActivity( act );
					act.setMaximumDuration( 0 );
				}

				l = factory.createLeg( TransportMode.walk );
				tt = random.nextDouble() * 1234;
				walkRoute = new GenericRouteImpl( doLink , DESTINATION_LINK );
				walkRoute.setTravelTime( tt );
				l.setTravelTime( tt );
				l.setRoute( walkRoute );
				p1Plan.addLeg( l );

				act = factory.createActivityFromLinkId( "w" , DESTINATION_LINK );
				act.setEndTime( (i * 12) + random.nextDouble() * 12 * 3600 );
				p1Plan.addActivity( act );

				l = factory.createLeg( TransportMode.walk );
				tt = random.nextDouble() * 1234;
				walkRoute = new GenericRouteImpl( DESTINATION_LINK , ORIGIN_LINK );
				walkRoute.setTravelTime( tt );
				l.setTravelTime( tt );
				l.setRoute( walkRoute );
				p1Plan.addLeg( l );

			}

			final Activity act = factory.createActivityFromLinkId( "h" , ORIGIN_LINK );
			p1Plan.addActivity( act );
		}

		return sc;
	}

	private static void createNetwork(final Network network) {
		int c = 0;
		int d = 0;

		final Node firstNode = network.getFactory().createNode( new IdImpl( c++ ) , new CoordImpl( 0 , d++ ) );
		Node node1 = firstNode;
		Node node2 = network.getFactory().createNode( new IdImpl( c++ ) , new CoordImpl( 0 , d++ ) );

		network.addNode( node1 );
		network.addNode( node2 );
		network.addLink( network.getFactory().createLink( ORIGIN_LINK , node1 , node2 ) );

		for (Id linkId : new Id[]{ TO_PU_LINK , PU_LINK , TRAVEL_LINK_1 , TRAVEL_LINK_2 , DO_LINK , TO_DESTINATION_LINK , DESTINATION_LINK }) {
			node1 = node2;
			node2 = network.getFactory().createNode( new IdImpl( c++ ) , new CoordImpl( 0 , d++ ) );
			network.addNode( node2 );
			network.addLink( network.getFactory().createLink( linkId , node1 , node2 ) );
		}

		network.addLink( network.getFactory().createLink( RETURN_LINK , node2 , firstNode ) );
	}
}

