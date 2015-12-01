/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingEventsToNumberOfEnRouteBikes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.framework.analysis;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.events.AgentStartsWaitingForFreeBikeSlotEvent;
import eu.eunoiaproject.bikesharing.framework.events.AgentStopsWaitingForBikeEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author thibautd
 */
public class BikeSharingEventsToNumberOfEnRouteBikes implements BasicEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final Map<Id, Double> depTimePerAgent = new HashMap<Id, Double>();
	private final SortedMap<Double, Integer> differentials = new TreeMap<Double, Integer>();

	@Override
	public void reset(final int iteration) {
		depTimePerAgent.clear();
		differentials.clear();
	}

	@Override
	public void handleEvent(final Event event) {

		if ( event.getEventType().equals( AgentStopsWaitingForBikeEvent.EVENT_TYPE ) ) {
			handleTypedEvent( new AgentStopsWaitingForBikeEvent( event ) );
		}

		if ( event.getEventType().equals( AgentStartsWaitingForFreeBikeSlotEvent.EVENT_TYPE ) ) {
			handleTypedEvent( new AgentStartsWaitingForFreeBikeSlotEvent( event ) );
		}

	}

	private void handleTypedEvent(
			final AgentStartsWaitingForFreeBikeSlotEvent event) {
		handleArrival( event.getPersonId() , event.getTime() );
	}

	private void handleTypedEvent(
			final AgentStopsWaitingForBikeEvent event) {
		depTimePerAgent.put( event.getPersonId() , event.getTime() );
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if ( event.getLegMode().equals( BikeSharingConstants.MODE ) ) {
			handleArrival( event.getPersonId() , event.getTime() );
		}
	}

	private void handleArrival(
			final Id personId,
			final double time) {
		final Double dep = depTimePerAgent.remove( personId );
		if ( dep == null ) return; // was processed when waiting

		modifyDifferential( dep.doubleValue() , +1 );
		modifyDifferential( time , -1 );
	}

	private void modifyDifferential(
			final double time,
			final int inc) {
		final Integer prev = differentials.get( time );
		differentials.put(
				time,
				prev == null ? inc : prev + inc );
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if ( event.getLegMode().equals( BikeSharingConstants.MODE ) ) {
			depTimePerAgent.put( event.getPersonId() , event.getTime() );
		}
	}

	public void writeFile( final String file ) {
		final BufferedWriter writer = IOUtils.getBufferedWriter( file );

		try {
			writer.write( "time\tenRoute" );
			writer.newLine();
			writer.write( "0\t0" );

			int curr = 0;
			for ( Map.Entry<Double, Integer> e : differentials.entrySet() ) {
				curr += e.getValue();
				writer.newLine();
				writer.write( e.getKey()+"\t"+curr );
			}

			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}


