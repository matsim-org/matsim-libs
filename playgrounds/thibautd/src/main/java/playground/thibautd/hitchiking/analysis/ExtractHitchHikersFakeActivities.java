/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractHitchHikersFakeActivities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.hitchiking.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import playground.thibautd.hitchiking.HitchHikingConstants;

/**
 * Parses an event file and creates fake activity starts and ends for the
 * following events:
 * <ul>
 * <li> "waitingTime" activity for the duration for which an agent waits for a driver
 * <li> departure and arrival activities ant pu and do
 * </ul>
 * @author thibautd
 */
public class ExtractHitchHikersFakeActivities {
	public static final String WAIT_ACT_TYPE = "waitingTime";
	public static final String DEP_ACT_TYPE = "passengerDeparture";
	public static final String ARR_ACT_TYPE = "passengerArrival";
	private static final double DUR = 60;

	public static void main(final String[] args) {
		String eventsFile = args[0];
		String outFile = args[1];

		Handler handler = new Handler( outFile );
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( handler );
		(new MatsimEventsReader( events )).readFile( eventsFile );
		handler.writer.closeFile();
	}

	private static class Handler implements BasicEventHandler, PersonArrivalEventHandler {
		private final EventWriterXML writer;

		public Handler(final String outFile) {
			writer = new EventWriterXML( outFile );
			writer.init( outFile );
		}

		@Override
		public void reset(final int iteration) {
		}

		@Override
		public void handleEvent(final PersonArrivalEvent event) {
			if (event.getLegMode().equals( HitchHikingConstants.PASSENGER_MODE )) {
				writer.handleEvent(
						new ActivityStartEvent(event.getTime(), event.getPersonId(), event.getLinkId(), null, ARR_ACT_TYPE));
				writer.handleEvent(
						new ActivityEndEvent(event.getTime() + DUR, event.getPersonId(), event.getLinkId(), null, ARR_ACT_TYPE));
			}
		}

		@Override
		public void handleEvent(final Event event) {
			if (event.getAttributes().get( Event.ATTRIBUTE_TYPE ).equals( "passengerStartsWaiting" )) {
				writer.handleEvent(
						new ActivityStartEvent(event.getTime(), Id.create( event.getAttributes().get( ActivityStartEvent.ATTRIBUTE_PERSON ) , Person.class), Id.create( event.getAttributes().get( "link" ) , Link.class ), null, WAIT_ACT_TYPE));
			}
			else if (event.getAttributes().get( Event.ATTRIBUTE_TYPE ).equals( "passengerEndsWaiting" )) {
				writer.handleEvent(
						new ActivityEndEvent(event.getTime(), Id.create( event.getAttributes().get( ActivityEndEvent.ATTRIBUTE_PERSON ) , Person.class ), Id.create( event.getAttributes().get( "link" ) , Link.class ), null, WAIT_ACT_TYPE));

				writer.handleEvent(
						new ActivityStartEvent(event.getTime(), Id.create( event.getAttributes().get( ActivityStartEvent.ATTRIBUTE_PERSON ) , Person.class ), Id.create( event.getAttributes().get( "link" ) , Link.class ), null, DEP_ACT_TYPE));
				writer.handleEvent(
						new ActivityEndEvent(event.getTime() + DUR, Id.create( event.getAttributes().get( ActivityEndEvent.ATTRIBUTE_PERSON ) , Person.class ), Id.create( event.getAttributes().get( "link" ) , Link.class ), null, DEP_ACT_TYPE));
			}		
		}
	}
}

