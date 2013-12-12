/* *********************************************************************** *
 * project: org.matsim.*
 * PSeudoQSimCompareEventsTest.java
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
package playground.thibautd.mobsim.pseudoqsimengine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.log4j.Logger;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.population.algorithms.PersonPrepareForSim;

import playground.thibautd.scripts.CreateGridNetworkWithDimensions;

/**
 * @author thibautd
 */
public class PSeudoQSimCompareEventsTest {
	private static final Logger log =
		Logger.getLogger(PSeudoQSimCompareEventsTest.class);

	@Test
	public void testEventsSimilarToQsim() {
		final Scenario scenario = createTestScenario();

		final EventsManager events = EventsUtils.createEventsManager();

		final EventStreamComparator handler = new EventStreamComparator();
		events.addHandler( handler );

		final TravelTimeCalculator travelTime =
			new TravelTimeCalculator(
					scenario.getNetwork(),
					scenario.getConfig().travelTimeCalculator());
		events.addHandler( travelTime );

		new PersonPrepareForSim(
				new PlanRouter(
					new TripRouterFactoryBuilderWithDefaults().build(
						scenario ).instantiateAndConfigureTripRouter(
							new RoutingContextImpl(
								new TravelTimeAndDistanceBasedTravelDisutility(
									travelTime.getLinkTravelTimes(),
									scenario.getConfig().planCalcScore() ),
								travelTime.getLinkTravelTimes() ) )
				),
				scenario).run( scenario.getPopulation() );


		log.info( "running actual simulation..." );
		final long startQSim = System.currentTimeMillis();
		new QSimFactory().createMobsim(
					scenario,
					events).run();
		final long timeQSim = System.currentTimeMillis() - startQSim;
		log.info( "running actual simulation... DONE" );

		handler.startCompare();

		log.info( "running pseudo simulation..." );
		final long startPSim = System.currentTimeMillis();
		new QSimWithPseudoEngineFactory(
					travelTime.getLinkTravelTimes()
				).createMobsim(
					scenario,
					events).run();
		final long timePSim = System.currentTimeMillis() - startPSim;
		log.info( "running pseudo simulation... DONE" );

		handler.assertNoMoreStoredEvents();

		log.info( "actual simulation took "+timeQSim+" ms." );
		log.info( "pseudo simulation took "+timePSim+" ms." );
	}

	private Scenario createTestScenario() {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		CreateGridNetworkWithDimensions.createNetwork(
				sc.getNetwork(),
				2,
				10 );

		final Random random = new Random( 1234 );
		final List<Id> linkIds = new ArrayList<Id>( sc.getNetwork().getLinks().keySet() );
		for ( int i = 0; i < 20; i++ ) {
			final Person person = sc.getPopulation().getFactory().createPerson( new IdImpl( i ) );
			sc.getPopulation().addPerson( person );

			final Plan plan = sc.getPopulation().getFactory().createPlan();
			person.addPlan( plan );

			final Activity firstActivity = 
					sc.getPopulation().getFactory().createActivityFromLinkId(
						"h",
						linkIds.get(
							random.nextInt(
								linkIds.size() ) ) );

			// everybody leaves at the same time, to have some congestion
			firstActivity.setEndTime( 10 );
			plan.addActivity( firstActivity );

			plan.addLeg( sc.getPopulation().getFactory().createLeg( TransportMode.car ) );

			plan.addActivity(
					sc.getPopulation().getFactory().createActivityFromLinkId(
						"h",
						linkIds.get(
							random.nextInt(
								linkIds.size() ) ) ) );

		}
		return sc;
	}

	private static class EventStreamComparator implements LinkEnterEventHandler,
			LinkLeaveEventHandler, Wait2LinkEventHandler, PersonEntersVehicleEventHandler,
			PersonLeavesVehicleEventHandler, BasicEventHandler {
		private boolean store = true;
		private final Map< Id , Queue<Event> > eventsPerPerson = new HashMap<Id, Queue<Event>>();

		@Override
		public void reset(int iteration) {
		}

		public void startCompare() {
			this.store = false;
		}

		public void assertNoMoreStoredEvents() {
			for ( Map.Entry<Id, Queue<Event>> entry : eventsPerPerson.entrySet() ) {
				final Id personId = entry.getKey();
				final Queue<Event> queue = entry.getValue();
				Assert.assertTrue(
						"still "+queue.size()+" stored events for person "+personId,
						queue.isEmpty() );
			}
		}

		private void handleEvent(
				final Id personId,
				final Event event) {
			if ( store ) {
				Queue<Event> queue = eventsPerPerson.get( personId ); 
				if ( queue == null ) {
					queue = new ArrayDeque<Event>();
					eventsPerPerson.put( personId , queue );
				}
				queue.add( event );
				return;
			}

			final Queue<Event> queue = eventsPerPerson.get( personId ); 
			Assert.assertFalse(
					"no more stored events for person "+personId,
					queue.isEmpty() );

			final Event storedEvent = queue.poll();

			Assert.assertEquals(
					"unexpected event type",
					storedEvent.getClass(),
					event.getClass() );

			// difficult to know what precision to ask...
			//Assert.assertEquals(
			//		"unexpected event time for "+event.getEventType(),
			//		storedEvent.getTime(),
			//		event.getTime(),
			//		2);

		}

		@Override
		public void handleEvent(final Event event) {
			if ( event instanceof HasPersonId ) {
				handleEvent(
						((HasPersonId) event).getPersonId(),
						event );
			}
		}

		@Override
		public void handleEvent(final PersonLeavesVehicleEvent event) {
			handleEvent(
				event.getPersonId(),
				event );
		}

		@Override
		public void handleEvent(final PersonEntersVehicleEvent event) {
			handleEvent(
				event.getPersonId(),
				event );
		}

		@Override
		public void handleEvent(final Wait2LinkEvent event) {
			handleEvent(
				event.getPersonId(),
				event );
		}

		@Override
		public void handleEvent(final LinkLeaveEvent event) {
			handleEvent(
				event.getPersonId(),
				event );
		}

		@Override
		public void handleEvent(final LinkEnterEvent event) {
			handleEvent(
				event.getPersonId(),
				event );
		}
	}
}

