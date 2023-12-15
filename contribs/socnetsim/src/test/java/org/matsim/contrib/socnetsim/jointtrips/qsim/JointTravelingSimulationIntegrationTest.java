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
package org.matsim.contrib.socnetsim.jointtrips.qsim;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author thibautd
 */
public class JointTravelingSimulationIntegrationTest {
	private static final Logger log =
		LogManager.getLogger(JointTravelingSimulationIntegrationTest.class);

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private static enum RouteType {
		normal,
		puAtDo,
		puAtDoFullCycle,
		everythingAtOrigin
	};

	// helps to understand test failures, but makes the test more expensive.
	// => to set to true when fixing tests only
	private static final boolean DUMP_EVENTS = false;

	private static final int N_RANDOM_SCENARIOS = 20;
	private static final int N_LAPS = 5;

	private final Id<Link> ORIGIN_LINK = Id.create("origin", Link.class);
	private final Id<Link> TO_PU_LINK = Id.create("toPu", Link.class);
	private final Id<Link> PU_LINK = Id.create("pu", Link.class);
	private final Id<Link> TRAVEL_LINK_1 = Id.create(1, Link.class);
	private final Id<Link> TRAVEL_LINK_2 = Id.create(2, Link.class);
	private final Id<Link> DO_LINK = Id.create("do", Link.class);
	private final Id<Link> TO_DESTINATION_LINK = Id.create("to_destination", Link.class);
	private final Id<Link> DESTINATION_LINK = Id.create("destination", Link.class);
	private final Id<Link> RETURN_LINK = Id.create("return", Link.class);

	private static final String ORIGIN_ACT = "chill";
	private static final String DESTINATION_ACT = "stress";

	@Test
	void testAgentsArriveTogetherWithoutDummies() throws Exception {
		testAgentsArriveTogether(
				createFixture(
					false,
					RouteType.normal ) );
	}

	@Test
	void testAgentsArriveTogetherWithDummies() throws Exception {
		testAgentsArriveTogether(
				createFixture(
					true,
					RouteType.normal ) );
	}

	@Test
	void testAgentsArriveTogetherWithDummiesAndDoAtPu() throws Exception {
		testAgentsArriveTogether(
				createFixture(
					true,
					RouteType.puAtDo ) );
	}

	@Test
	void testAgentsArriveTogetherWithoutDummiesAndDoAtPu() throws Exception {
		testAgentsArriveTogether(
				createFixture(
					false,
					RouteType.puAtDo ) );
	}

	@Test
	void testAgentsArriveTogetherWithDummiesAndDoAtPuFullCycle() throws Exception {
		testAgentsArriveTogether(
				createFixture(
					true,
					RouteType.puAtDoFullCycle ) );
	}

	@Test
	void testAgentsArriveTogetherWithoutDummiesAndDoAtPuFullCycle() throws Exception {
		testAgentsArriveTogether(
				createFixture(
					false,
					RouteType.puAtDoFullCycle ) );
	}

	@Test
	void testAgentsArriveTogetherWithDummiesAndEverythingAtOrigin() throws Exception {
		testAgentsArriveTogether(
				createFixture(
					true,
					RouteType.everythingAtOrigin ) );
	}

	@Test
	void testAgentsArriveTogetherWithoutDummiesAndEverythingAtOrigin() throws Exception {
		testAgentsArriveTogether(
				createFixture(
					false,
					RouteType.everythingAtOrigin ) );
	}


	private void testAgentsArriveTogether( final Fixture fixture ) throws Exception {
		final Random random = new Random( 1234 );

		// To make the output more readable (otherwise, warning that driver mode
		// is added as a main mode is logged each time)
		for (int i=0; i < N_RANDOM_SCENARIOS; i++) {
			log.info( "random test scenario "+i );
			final Scenario sc =
				createTestScenario(
						fixture,
						random);
			final EventsManager events = EventsUtils.createEventsManager();

			final EventWriter eventsWriter = new EventWriterXML( utils.getOutputDirectory()+"/events.xml.gz" );
			if ( DUMP_EVENTS ) {
				// do listen to events only if we are actually interested in them
				events.addHandler( eventsWriter );
			}

			final AtomicInteger arrCount = new AtomicInteger( 0 );
			final int scNr = i;
			events.addHandler( new PersonArrivalEventHandler() {
				private double arrival = -100;
				private int nArrival = 0;

				@Override
				public void reset(final int iteration) {}

				@Override
				public void handleEvent(final PersonArrivalEvent event) {
					final String mode = event.getLegMode();
					log.info( "agent "+event.getPersonId()+": "+mode+" arrival at "+event.getTime() );
					if ( mode.equals( JointActingTypes.DRIVER ) ||
							mode.equals( JointActingTypes.PASSENGER ) ) {
						if ( nArrival++ % 3 == 0 ) {
							arrival = event.getTime();
							assert arrival > 0;
						}

						arrCount.incrementAndGet();
						Assertions.assertEquals(
							arrival,
							event.getTime(),
							MatsimTestUtils.EPSILON,
							"run "+scNr+": unexpected joint arrival time");

						Assertions.assertEquals(
								fixture.doLink,
								event.getLinkId(),
								"run "+scNr+": unexpected arrival location for mode "+mode );
					}
				}
			});
			final AtomicInteger atDestCount = new AtomicInteger( 0 );
			events.addHandler( new ActivityStartEventHandler() {
				@Override
				public void reset(final int iteration) {}

				@Override
				public void handleEvent(final ActivityStartEvent event) {
					log.info( "agent "+event.getPersonId()+": "+event.getActType()+" start at "+event.getTime() );
					if ( event.getActType().equals( DESTINATION_ACT ) ) {
						atDestCount.incrementAndGet();
					}
				}
			});

			final JointQSimFactory factory = new JointQSimFactory();
			final QSim qsim = factory.createMobsim( sc , events );
			try {
				qsim.run();
			}
			catch ( AssertionError t ) {
				events.processEvent( new RunAbortedEvent() );
				throw t;
			}
			finally {
				eventsWriter.closeFile();
			}

			// for easier tracking of test failures
			logFinalQSimState( qsim );

			Assertions.assertEquals(
					N_LAPS * 3,
					arrCount.get(),
					"run "+i+": unexpected number of joint arrivals");

			Assertions.assertEquals(
					N_LAPS * sc.getPopulation().getPersons().size(),
					atDestCount.get(),
					"run "+i+": unexpected number of agents arriving at destination");
		}
	}

	private static void logFinalQSimState(final QSim qsim) {
		for ( MobsimAgent agent : qsim.getAgents().values() ) {
			log.info( "agent state at end: "+agent );
		}
	}

	@Test
	void testNumberOfEnterLeaveVehicle() {
		testNumberOfEnterLeaveVehicle( RouteType.normal );
	}

	@Test
	void testNumberOfEnterLeaveVehicleEverythingAtOrigin() {
		testNumberOfEnterLeaveVehicle( RouteType.everythingAtOrigin );
	}

	@Test
	void testNumberOfEnterLeaveVehiclePuAtDo() {
		testNumberOfEnterLeaveVehicle( RouteType.puAtDo );
	}

	@Test
	void testNumberOfEnterLeaveVehiclePuAtDoFullCycle() {
		testNumberOfEnterLeaveVehicle( RouteType.puAtDoFullCycle );
	}

	private void testNumberOfEnterLeaveVehicle(final RouteType routeType) {
		final Random random = new Random( 1234 );

		for (int i=0; i < N_RANDOM_SCENARIOS; i++) {
			log.info( "random test scenario "+i );
			final Scenario sc = createTestScenario( createFixture( false , routeType ) , random );
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

			Assertions.assertEquals(
					enterCount.get(),
					leaveCount.get(),
					"not as many leave events as enter events");
		}
	}

	private Fixture createFixture(
			final boolean insertDummyActivities,
			final RouteType routeType) {
		switch ( routeType ) {
			case normal:
				return new Fixture(
						insertDummyActivities,
						ORIGIN_LINK,
						PU_LINK,
						DO_LINK,
						DESTINATION_LINK,
						Arrays.asList( TO_PU_LINK ),
						Arrays.asList( TRAVEL_LINK_1 , TRAVEL_LINK_2 ),
						Arrays.asList( TO_DESTINATION_LINK ),
						Arrays.asList( RETURN_LINK ));
			case puAtDo:
				return new Fixture(
						insertDummyActivities,
						ORIGIN_LINK,
						PU_LINK,
						PU_LINK,
						DESTINATION_LINK,
						Arrays.asList( TO_PU_LINK ),
						Collections.<Id<Link>> emptyList(),
						Arrays.asList(
							TRAVEL_LINK_1,
							TRAVEL_LINK_2,
							DO_LINK,
							TO_DESTINATION_LINK ),
						Arrays.asList( RETURN_LINK ));
			case puAtDoFullCycle:
				return new Fixture(
						insertDummyActivities,
						ORIGIN_LINK,
						PU_LINK,
						PU_LINK,
						DESTINATION_LINK,
						Arrays.asList( TO_PU_LINK ),
						Arrays.asList(
							TRAVEL_LINK_1,
							TRAVEL_LINK_2,
							DO_LINK,
							TO_DESTINATION_LINK,
							DESTINATION_LINK,
							RETURN_LINK,
							ORIGIN_LINK,
							TO_PU_LINK ),
						Arrays.asList(
							TRAVEL_LINK_1,
							TRAVEL_LINK_2,
							DO_LINK,
							TO_DESTINATION_LINK ),
						Arrays.asList( RETURN_LINK ));
			case everythingAtOrigin:
					return new Fixture(
							insertDummyActivities,
							ORIGIN_LINK,
							ORIGIN_LINK,
							ORIGIN_LINK,
							ORIGIN_LINK,
							Collections.<Id<Link>> emptyList(),
							Collections.<Id<Link>> emptyList(),
							Collections.<Id<Link>> emptyList(),
							Collections.<Id<Link>> emptyList());
			default:
				throw new RuntimeException( routeType.toString() );
		}
	}

	private Scenario createTestScenario(
			final Fixture fixture,
			final Random random) {
		final Config config = ConfigUtils.createConfig();
		final QSimConfigGroup qsimConf = config.qsim();
		qsimConf.setEndTime( 30 * 3600 );
		final Scenario sc = ScenarioUtils.createScenario( config );

		createNetwork( sc.getNetwork() );

		final Id<Person> driverId = Id.create( "driver" , Person.class );
		final Id<Person> passengerId1 = Id.create( "passenger_1" , Person.class);
		final Id<Person> passengerId2 = Id.create( "passenger_2" , Person.class);

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
				Activity act = factory.createActivityFromLinkId( ORIGIN_ACT , fixture.originLink );
				act.setEndTime( (i * 12) + random.nextDouble() * 6 * 3600 );
				driverPlan.addActivity( act );

				Leg l = factory.createLeg( TransportMode.car );
				l.setRoute( RouteUtils.createLinkNetworkRouteImpl(fixture.originLink, fixture.toPuRoute, fixture.puLink) );
				driverPlan.addLeg( l );

				if ( fixture.insertDummyActivities ) {
					act = factory.createActivityFromLinkId(
								JointActingTypes.INTERACTION,
								fixture.puLink );
					driverPlan.addActivity( act );
					act.setMaximumDuration( 0 );
				}

				l = factory.createLeg( JointActingTypes.DRIVER );
				DriverRoute dRoute =
					new DriverRoute(
							fixture.puLink,
							fixture.doLink );
				dRoute.setLinkIds(
						fixture.puLink ,
						fixture.puToDoRoute,
						fixture.doLink);
				dRoute.addPassenger( passengerId1 );
				dRoute.addPassenger( passengerId2 );
				l.setRoute( dRoute );
				driverPlan.addLeg( l );

				if ( fixture.insertDummyActivities ) {
					act = factory.createActivityFromLinkId(
								JointActingTypes.INTERACTION,
								fixture.doLink );
					driverPlan.addActivity( act );
					act.setMaximumDuration( 0 );
				}

				l = factory.createLeg( TransportMode.car );
				l.setRoute( RouteUtils.createLinkNetworkRouteImpl(fixture.doLink, fixture.doToDestRoute, fixture.destinationLink) );
				driverPlan.addLeg( l );

				final Activity work = factory.createActivityFromLinkId( DESTINATION_ACT , fixture.destinationLink );
				work.setEndTime( (i * 12) + random.nextDouble() * 12 * 3600 );
				driverPlan.addActivity( work );

				l = factory.createLeg( TransportMode.car );
				l.setRoute(
						RouteUtils.createLinkNetworkRouteImpl(fixture.destinationLink, fixture.destToOrigRoute,
								fixture.originLink) );
				driverPlan.addLeg( l );
			}

			final Activity act = factory.createActivityFromLinkId( ORIGIN_ACT , fixture.originLink );
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
				Activity act = factory.createActivityFromLinkId( ORIGIN_ACT , fixture.originLink );
				act.setEndTime( (i * 12) + random.nextDouble() * 6 * 3600 );
				p1Plan.addActivity( act );

				Leg l = factory.createLeg( TransportMode.walk );
				double tt = random.nextDouble() * 1234;
				Route walkRoute = RouteUtils.createGenericRouteImpl(fixture.originLink, fixture.puLink);
				walkRoute.setTravelTime( tt );
				l.setTravelTime( tt );
				l.setRoute( walkRoute );
				p1Plan.addLeg( l );

				if ( fixture.insertDummyActivities ) {
					act = factory.createActivityFromLinkId(
								JointActingTypes.INTERACTION,
								fixture.puLink );
					p1Plan.addActivity( act );
					act.setMaximumDuration( 0 );
				}

				l = factory.createLeg( JointActingTypes.PASSENGER );
				PassengerRoute pRoute =
					new PassengerRoute(
							fixture.puLink,
							fixture.doLink );
				pRoute.setDriverId( driverId );
				l.setRoute( pRoute );
				p1Plan.addLeg( l );

				if ( fixture.insertDummyActivities ) {
					act = factory.createActivityFromLinkId(
								JointActingTypes.INTERACTION,
								fixture.doLink );
					p1Plan.addActivity( act );
					act.setMaximumDuration( 0 );
				}

				l = factory.createLeg( TransportMode.walk );
				tt = random.nextDouble() * 1234;
				walkRoute = RouteUtils.createGenericRouteImpl(fixture.doLink, fixture.destinationLink);
				walkRoute.setTravelTime( tt );
				l.setTravelTime( tt );
				l.setRoute( walkRoute );
				p1Plan.addLeg( l );

				act = factory.createActivityFromLinkId( DESTINATION_ACT , fixture.destinationLink );
				act.setEndTime( (i * 12) + random.nextDouble() * 12 * 3600 );
				p1Plan.addActivity( act );

				l = factory.createLeg( TransportMode.walk );
				tt = random.nextDouble() * 1234;
				walkRoute = RouteUtils.createGenericRouteImpl(fixture.destinationLink, fixture.originLink);
				walkRoute.setTravelTime( tt );
				l.setTravelTime( tt );
				l.setRoute( walkRoute );
				p1Plan.addLeg( l );

			}

			final Activity act = factory.createActivityFromLinkId( ORIGIN_ACT , fixture.originLink );
			p1Plan.addActivity( act );
		}

		return sc;
	}

	private void createNetwork(final Network network) {
		int c = 0;
		int d = 0;

		final double y2 = d++;
		final Node firstNode = network.getFactory().createNode( Id.create( c++ , Node.class ) , new Coord((double) 0, y2));
		Node node1 = firstNode;
		final double y1 = d++;
		Node node2 = network.getFactory().createNode( Id.create( c++ , Node.class ) , new Coord((double) 0, y1));

		network.addNode( node1 );
		network.addNode( node2 );
		network.addLink( network.getFactory().createLink( ORIGIN_LINK , node1 , node2 ) );

		for (Id<Link> linkId : new Id[]{ TO_PU_LINK , PU_LINK , TRAVEL_LINK_1 , TRAVEL_LINK_2 , DO_LINK , TO_DESTINATION_LINK , DESTINATION_LINK }) {
			node1 = node2;
			final double y = d++;
			node2 = network.getFactory().createNode( Id.create( c++ , Node.class ) , new Coord((double) 0, y));
			network.addNode( node2 );
			network.addLink( network.getFactory().createLink( linkId , node1 , node2 ) );
		}

		network.addLink( network.getFactory().createLink( RETURN_LINK , node2 , firstNode ) );
	}

	private static class Fixture {
		final private boolean insertDummyActivities;
		final private Id<Link> originLink;
		final private Id<Link> puLink;
		final private Id<Link> doLink;
		final private Id<Link> destinationLink;
		final private List<Id<Link>> toPuRoute;
		final private List<Id<Link>> puToDoRoute;
		final private List<Id<Link>> doToDestRoute;
		final private List<Id<Link>> destToOrigRoute;

		public Fixture(
				final boolean insertDummyActivities,
				final Id<Link> originLink,
				final Id<Link> puLink,
				final Id<Link> doLink,
				final Id<Link> destinationLink,
				final List<Id<Link>> toPuRoute,
				final List<Id<Link>> puToDoRoute,
				final List<Id<Link>> doToDestRoute,
				final List<Id<Link>> destToOrigRoute) {
			this.insertDummyActivities = insertDummyActivities;
			this.originLink = originLink;
			this.puLink = puLink;
			this.doLink = doLink;
			this.destinationLink = destinationLink;
			this.toPuRoute = toPuRoute;
			this.puToDoRoute = puToDoRoute;
			this.doToDestRoute = doToDestRoute;
			this.destToOrigRoute = destToOrigRoute;
		}
	}

	private static class RunAbortedEvent extends Event {
		public RunAbortedEvent() {
			super( Double.NaN );
		}

		@Override
		public String getEventType() {
			return "RunAborted";
		}
	}
}

