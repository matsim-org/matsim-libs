/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingEventsToWaitStatistics.java
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
import eu.eunoiaproject.bikesharing.framework.events.AgentStartsWaitingForBikeEvent;
import eu.eunoiaproject.bikesharing.framework.events.AgentStartsWaitingForFreeBikeSlotEvent;
import eu.eunoiaproject.bikesharing.framework.events.AgentStopsWaitingForBikeEvent;
import eu.eunoiaproject.bikesharing.framework.events.AgentStopsWaitingForFreeBikeSlotEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
public class BikeSharingEventsToWaitStatistics implements BasicEventHandler, PersonDepartureEventHandler, ActivityStartEventHandler {

	private final Map<Id, String> lastModePerAgent = new HashMap<Id, String>();
	private final Map<Id, Integer> startBikeWaitingPerStation = new HashMap<Id, Integer>();
	private final Map<Id, Integer> stopBikeWaitingPerStation = new HashMap<Id, Integer>();
	private final Map<Id, Integer> departureInteractionPerStation = new HashMap<Id, Integer>();

	private final Map<Id, Integer> startSlotWaitingPerStation = new HashMap<Id, Integer>();
	private final Map<Id, Integer> stopSlotWaitingPerStation = new HashMap<Id, Integer>();
	private final Map<Id, Integer> arrivalInteractionPerStation = new HashMap<Id, Integer>();

	@Override
	public void reset(final int iteration) {
		lastModePerAgent.clear();
		startBikeWaitingPerStation.clear();
		stopBikeWaitingPerStation.clear();
		departureInteractionPerStation.clear();

		startSlotWaitingPerStation.clear();
		stopSlotWaitingPerStation.clear();
		arrivalInteractionPerStation.clear();
	}

	@Override
	public void handleEvent(final Event event) {

		if ( event.getEventType().equals( AgentStartsWaitingForBikeEvent.EVENT_TYPE ) ) {
			handleTypedEvent( new AgentStartsWaitingForBikeEvent( event ) );
		}

		if ( event.getEventType().equals( AgentStopsWaitingForBikeEvent.EVENT_TYPE ) ) {
			handleTypedEvent( new AgentStopsWaitingForBikeEvent( event ) );
		}

		if ( event.getEventType().equals( AgentStartsWaitingForFreeBikeSlotEvent.EVENT_TYPE ) ) {
			handleTypedEvent( new AgentStartsWaitingForFreeBikeSlotEvent( event ) );
		}

		if ( event.getEventType().equals( AgentStopsWaitingForFreeBikeSlotEvent.EVENT_TYPE ) ) {
			handleTypedEvent( new AgentStopsWaitingForFreeBikeSlotEvent( event ) );
		}
	}

	private void handleTypedEvent(
			final AgentStopsWaitingForFreeBikeSlotEvent event) {
		increment( stopSlotWaitingPerStation , event.getFacilityId() );
	}

	private void handleTypedEvent(
			final AgentStopsWaitingForBikeEvent event) {
		increment( stopBikeWaitingPerStation , event.getFacilityId() );
	}

	private void handleTypedEvent(
			final AgentStartsWaitingForFreeBikeSlotEvent event) {
		increment( startSlotWaitingPerStation , event.getFacilityId() );
	}

	private void handleTypedEvent(
			final AgentStartsWaitingForBikeEvent event) {
		increment( startBikeWaitingPerStation , event.getFacilityId() );
	}

	private static void increment(
			final Map<Id, Integer> map,
			final Id key ) {
		final Integer curr = map.get( key );
		map.put( key , curr == null ? 1 : curr + 1 );
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		lastModePerAgent.put( event.getPersonId() ,  event.getLegMode() );
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if ( event.getActType().equals( BikeSharingConstants.INTERACTION_TYPE ) ) {
			final String mode = lastModePerAgent.remove( event.getPersonId() );
			if ( mode.equals( TransportMode.walk ) ) {
				increment( departureInteractionPerStation , event.getFacilityId() );
			}
			else if ( mode.equals( BikeSharingConstants.MODE ) ) {
				increment( arrivalInteractionPerStation , event.getFacilityId() );
			}
			else {
				throw new IllegalStateException( mode+" "+event );
			}
		}
	}

	public void writeFile( final String file ) {
		final BufferedWriter writer = IOUtils.getBufferedWriter( file );

		try {
			writer.write( "stationId\ttotalDepartures\tdelayedDepartures\tabortedDepartures\ttotalArrivals\tdelayedArrivals\tabortedArrivals" );
			writer.newLine();

			final Set<Id> knownStations = new HashSet<Id>();
			knownStations.addAll( this.stopSlotWaitingPerStation.keySet() );
			knownStations.addAll( this.stopBikeWaitingPerStation.keySet() );
			knownStations.addAll( this.arrivalInteractionPerStation.keySet() );
			knownStations.addAll( this.departureInteractionPerStation.keySet() );

			for ( Id station : knownStations ) {
				final int tDeps = get( departureInteractionPerStation , station );
				final int dDeps = get( stopBikeWaitingPerStation , station );
				final int wDeps = get( startBikeWaitingPerStation , station );

				final int tArrivals = get( arrivalInteractionPerStation , station );
				final int dArrivals = get( stopSlotWaitingPerStation, station );
				final int wArrivals = get( startSlotWaitingPerStation , station );

				writer.newLine();
				writer.write( station.toString() );
				writer.write( "\t"+tDeps ); // all departures, even "aborted", start with interaction
				writer.write( "\t"+dDeps );
				writer.write( "\t"+( wDeps - dDeps ) ); // "aborted" are agents who started waiting and never ended
				writer.write( "\t"+( tArrivals + wArrivals - dArrivals ) ); // aborted departures do not generate interaction
				writer.write( "\t"+dArrivals );
				writer.write( "\t"+( wArrivals - dArrivals ) );
			}

			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private int get(final Map<Id, Integer> map, final Id key) {
		return map.containsKey( key ) ? map.get( key ) : 0;
	}
}


