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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * @author thibautd
 */
public class CompareEventsUtils {
	private static final Logger log =
		Logger.getLogger(CompareEventsUtils.class);

	public static void testEventsSimilarToQsim(
			final Scenario scenario,
			final PlanAlgorithm planRouter,
			final MobsimFactory qSimFactory,
			final MobsimFactory pseudoSimFactory,
			final TravelTimeCalculator travelTime ) {
		testEventsSimilarToQsim(
				scenario,
				planRouter,
				qSimFactory,
				null,
				pseudoSimFactory,
				null,
				travelTime,
				false );
	}

	/**
	 * public to test specific implementations of the factory from the
	 * respective packages
	 */
	public static void testEventsSimilarToQsim(
			final Scenario scenario,
			final PlanAlgorithm planRouter,
			final MobsimFactory qSimFactory,
			final String eventsFileQSim,
			final MobsimFactory pseudoSimFactory,
			final String eventsFilePSim,
			final TravelTimeCalculator travelTime,
			final boolean ignoreLinkEvents) {

		final EventsManager events = EventsUtils.createEventsManager();

		final EventStreamComparator handler = new EventStreamComparator( ignoreLinkEvents );
		events.addHandler( handler );

		events.addHandler( travelTime );


		new PersonPrepareForSim(
				planRouter,
				scenario).run( scenario.getPopulation() );

		long timeQSim, timePSim;

		/* scope for qSim */ {
			final EventWriterXML writer = 
				eventsFileQSim != null ?
					new EventWriterXML( eventsFileQSim ) :
					null;

			try {
				log.info( "running reference simulation..." );
				if ( writer != null ) events.addHandler( writer );
				final long startQSim = System.currentTimeMillis();
				qSimFactory.createMobsim(
							scenario,
							events).run();
				timeQSim = System.currentTimeMillis() - startQSim;
				if ( writer != null ) {
					events.removeHandler( writer );
				}
				log.info( "running reference simulation... DONE" );
			}
			catch ( Error e ) {
				throw e;
			}
			finally {
				if (writer != null) writer.closeFile();
			}
		}

		handler.startCompare();


		/* scope for pSim */ {
			final EventWriterXML writer = 
				eventsFilePSim != null ?
					new EventWriterXML( eventsFilePSim ) :
					null;

			try {
				log.info( "running tested simulation..." );
				if ( writer != null ) events.addHandler( writer );
				final long startPSim = System.currentTimeMillis();
				pseudoSimFactory.createMobsim(
							scenario,
							events).run();
				timePSim = System.currentTimeMillis() - startPSim;
				if ( writer != null ) {
					events.removeHandler( writer );
				}
				log.info( "running tested simulation... DONE" );
			}
			catch ( Error e ) {
				throw e;
			}
			finally {
				if (writer != null) writer.closeFile();
			}
		}

		handler.assertNoMoreStoredEvents();

		log.info( "reference simulation took "+timeQSim+" ms." );
		log.info( "tested simulation took "+timePSim+" ms." );
	}

	private static class EventStreamComparator implements
			LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler,
			VehicleLeavesTrafficEventHandler,
			PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
			TransitDriverStartsEventHandler, AgentWaitingForPtEventHandler,
			VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler,
			BasicEventHandler {
		private boolean store = true;
		private final Map< Id , Queue<Event> > eventsPerPerson = new HashMap<Id, Queue<Event>>();
		private final Map< Id , Queue<Event> > eventsPerVehicle = new HashMap<Id, Queue<Event>>();

		private final boolean ignoreLinkEvents;

		public EventStreamComparator(final boolean ignoreLinkEvents) {
			this.ignoreLinkEvents = ignoreLinkEvents;
		}

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
				final Map< Id, Queue<Event> > eventsMap,
				final Id queueId,
				final Event event) {
			if ( store ) {
				Queue<Event> queue = eventsMap.get( queueId ); 
				if ( queue == null ) {
					queue = new ArrayDeque<Event>();
					eventsMap.put( queueId , queue );
				}
				queue.add( event );
				return;
			}

			final Queue<Event> queue = eventsMap.get( queueId ); 
			Assert.assertFalse(
					"no more stored events for id "+queueId,
					queue.isEmpty() );

			final Event storedEvent = queue.poll();

			if ( log.isTraceEnabled() ) {
				log.trace( "person "+queueId+": compare stored event "+
						storedEvent+" with event "+event );
			}

			Assert.assertEquals(
					"unexpected event type for id "+queueId,
					storedEvent.getClass(),
					event.getClass() );

			if ( event instanceof LinkEnterEvent ) {
				Assert.assertEquals(
						"unexpected entered link id person "+queueId,
						((LinkEnterEvent) storedEvent).getLinkId(),
						((LinkEnterEvent) event).getLinkId() );
			}
			if ( event instanceof LinkLeaveEvent ) {
				Assert.assertEquals(
						"unexpected left link id person "+queueId,
						((LinkLeaveEvent) storedEvent).getLinkId(),
						((LinkLeaveEvent) event).getLinkId() );
			}
			if ( event instanceof VehicleEntersTrafficEvent ) {
				Assert.assertEquals(
						"unexpected wait link id person "+queueId,
						((VehicleEntersTrafficEvent) storedEvent).getLinkId(),
						((VehicleEntersTrafficEvent) event).getLinkId() );
			}

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
						eventsPerPerson,
						((HasPersonId) event).getPersonId(),
						event );
			}
		}

		@Override
		public void handleEvent(final PersonLeavesVehicleEvent event) {
			handleEvent(
				eventsPerPerson,
				event.getPersonId(),
				event );
		}

		@Override
		public void handleEvent(final PersonEntersVehicleEvent event) {
			handleEvent(
				eventsPerPerson,
				event.getPersonId(),
				event );
		}

		@Override
		public void handleEvent(final VehicleEntersTrafficEvent event) {
			if ( ignoreLinkEvents ) return;
			handleEvent(
				eventsPerPerson,
				event.getPersonId(),
				event );
		}

		@Override
		public void handleEvent(final LinkLeaveEvent event) {
			if ( ignoreLinkEvents ) return;
			handleEvent(
				eventsPerPerson,
				event.getDriverId(),
				event );
		}

		@Override
		public void handleEvent(final LinkEnterEvent event) {
			if ( ignoreLinkEvents ) return;
			handleEvent(
				eventsPerPerson,
				event.getDriverId(),
				event );
		}

		@Override
		public void handleEvent(final VehicleArrivesAtFacilityEvent event) {
			handleEvent(
				eventsPerVehicle,
				event.getVehicleId(),
				event );
		}

		@Override
		public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
			handleEvent(
				eventsPerVehicle,
				event.getVehicleId(),
				event );
		}

		@Override
		public void handleEvent(final AgentWaitingForPtEvent event) {
			handleEvent(
				eventsPerPerson,
				event.getPersonId(),
				event );
		}

		@Override
		public void handleEvent(final TransitDriverStartsEvent event) {
			handleEvent(
				eventsPerVehicle,
				event.getVehicleId(),
				event );
		}

		@Override
		public void handleEvent(final VehicleLeavesTrafficEvent event) {
			if ( ignoreLinkEvents ) return;
			handleEvent(
				eventsPerPerson,
				event.getPersonId(),
				event );
		}
	}

}

