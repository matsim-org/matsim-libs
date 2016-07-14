/* *********************************************************************** *
 * project: org.matsim.*
 * TripsRecordsEventsHandler.java
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
package playground.thibautd.analysis.spacetimeprismjoinabletrips;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.PtConstants;
import playground.thibautd.analysis.joinabletripsidentifier.Trip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses events to get trip records.
 * PT trips with transfers are identified as one single trip
 * @author thibautd
 */
public class TripsRecordsEventsHandler implements
		PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler {
	private final Map<Id<Person>, RecordBuilder> personInfo = new HashMap<>();
	private final Map<Id<Person>, TripCounter> personCounters = new HashMap<>();
	private List<Record> records = new ArrayList<Record>();

	@Override
	public void reset(final int iteration) {
		records = new ArrayList<Record>();
		personInfo.clear();
		personCounters.clear();
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if (!event.getActType().equals( PtConstants.TRANSIT_ACTIVITY_TYPE )) {
			records.add( personInfo.remove( event.getPersonId() ).build() );
		}
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		final RecordBuilder builder = personInfo.get( event.getPersonId() );
		
		builder.setArrivalTime( event.getTime() );
		builder.setDestinationLink( event.getLinkId() );
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		RecordBuilder builder = personInfo.get( event.getPersonId() );

		if (builder == null) {
			builder = new RecordBuilder( event.getPersonId() );
			builder.setDepartureTime( event.getTime() );
			builder.setOriginLink( event.getLinkId() );
			builder.setTripMode( event.getLegMode() );
			builder.setTripNr( nextTripCount( event.getPersonId() ) );
			personInfo.put( event.getPersonId() , builder );
		}

		if (event.getLegMode().equals( TransportMode.pt )) {
			builder.setTripMode( event.getLegMode() );
		}
	}

	public List<Record> getRecords() {
		return records;
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private int nextTripCount(final Id<Person> person) {
		TripCounter c = personCounters.get( person );

		if (c == null) {
			c = new TripCounter();
			personCounters.put( person , c );
		}

		return c.nextCount();
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class TripCounter {
		private int c = 0;

		public int nextCount() {
			return c++;
		}
	}

	private static class RecordBuilder {
		private static long currentId = Long.MIN_VALUE;
		private int tripNr;
		private final Id<Person> agentId;
		private Id<Link> originLink, destinationLink;
		private double departureTime, arrivalTime;
		private String tripMode;

		public RecordBuilder(final Id<Person> agentId) {
			this.agentId = agentId;
		}

		public void setTripNr(final int tripNr) {
			this.tripNr = tripNr;
		}

		public void setOriginLink(final Id<Link> originLink) {
			this.originLink = originLink;
		}

		public void setDestinationLink(final Id<Link> destinationLink) {
			this.destinationLink = destinationLink;
		}

		public void setDepartureTime(final double departureTime) {
			this.departureTime = departureTime;
		}

		public void setArrivalTime(final double arrivalTime) {
			this.arrivalTime = arrivalTime;
		}

		public void setTripMode(final String tripMode) {
			this.tripMode = tripMode;
		}

		public synchronized Record build() {
			return new Record( Id.create( currentId++ , Trip.class ), tripNr, tripMode,
					agentId, originLink, destinationLink, departureTime, arrivalTime);
		}
	}
}

