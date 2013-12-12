/* *********************************************************************** *
 * project: org.matsim.*
 * CompareEventsUtils.java
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
package playground.thibautd.mobsim;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;

import org.junit.Assert;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author thibautd
 */
public class CompareEventsUtils {
	private static final Logger log =
		Logger.getLogger(CompareEventsUtils.class);

	/**
	 * public to test specific implementations of the factory from the
	 * respective packages
	 */
	public static void testEventsSimilarToQsim(
			final Scenario scenario,
			final PlanAlgorithm planRouter,
			final MobsimFactory qSimFactory,
			final MobsimFactory pseudoSimFactory,
			final TravelTimeCalculator travelTime ) {

		final EventsManager events = EventsUtils.createEventsManager();

		final EventStreamComparator handler = new EventStreamComparator();
		events.addHandler( handler );

		events.addHandler( travelTime );

		new PersonPrepareForSim(
				planRouter,
				scenario).run( scenario.getPopulation() );


		log.info( "running reference simulation..." );
		final long startQSim = System.currentTimeMillis();
		qSimFactory.createMobsim(
					scenario,
					events).run();
		final long timeQSim = System.currentTimeMillis() - startQSim;
		log.info( "running reference simulation... DONE" );

		handler.startCompare();

		log.info( "running tested simulation..." );
		final long startPSim = System.currentTimeMillis();
		pseudoSimFactory.createMobsim(
					scenario,
					events).run();
		final long timePSim = System.currentTimeMillis() - startPSim;
		log.info( "running tested simulation... DONE" );

		handler.assertNoMoreStoredEvents();

		log.info( "reference simulation took "+timeQSim+" ms." );
		log.info( "tested simulation took "+timePSim+" ms." );
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

			if ( log.isTraceEnabled() ) {
				log.trace( "person "+personId+": compare stored event "+
						storedEvent+" with event "+event );
			}

			Assert.assertEquals(
					"unexpected event type for person "+personId,
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

