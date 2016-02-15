/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingEventsToXyTable.java
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
package eu.eunoiaproject.bikesharing.framework.analysis;

import eu.eunoiaproject.bikesharing.framework.events.AgentStartsWaitingForBikeEvent;
import eu.eunoiaproject.bikesharing.framework.events.AgentStartsWaitingForFreeBikeSlotEvent;
import eu.eunoiaproject.bikesharing.framework.events.AgentStopsWaitingForBikeEvent;
import eu.eunoiaproject.bikesharing.framework.events.AgentStopsWaitingForFreeBikeSlotEvent;
import eu.eunoiaproject.bikesharing.framework.events.NewBikeSharingFacilityStateEvent;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An event handler which listens to a stream of events and dumps bike sharing
 * statistics in tabular format. The table can be read from via to visualize
 * loads at bike sharing stations.
 * <br>
 * It is done to be usable off-line (ie when re-reading the events after the end
 * of the simulation: the events do not need to be of the specific implementation,
 * but can be generic events.)
 * @author thibautd
 */
public class BikeSharingEventsToXyTable implements BasicEventHandler {
	private final BikeSharingFacilities facilities;
	private final BufferedWriter writer;

	private final Map<Id, Integer> waitingForBikeAgents = new HashMap<Id, Integer>();
	private final Map<Id, Integer> waitingForSlotAgents = new HashMap<Id, Integer>();
	private final Map<Id, Integer> bikesAtStation = new HashMap<Id, Integer>();

	public BikeSharingEventsToXyTable(
			final BikeSharingFacilities facilities,
			final BufferedWriter writer) {
		this.facilities = facilities;
		this.writer = writer;

		try {
			writer.write( "stationId\tx\ty\ttime\tnBikes\tcapacity\tnWaitingForBikeAgents\tnWaitingForSlotAgents" );

			for ( BikeSharingFacility facility : facilities.getFacilities().values() ) {
				writer.newLine();
				writer.write( facility.getId()+"\t"+
						facility.getCoord().getX()+"\t"+
						facility.getCoord().getY()+"\t"+
						"0\t"+
						facility.getInitialNumberOfBikes()+"\t"+
						facility.getCapacity()+"\t"+
						"0\t0");
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void reset(final int iteration) {}

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

		if ( event.getEventType().equals( NewBikeSharingFacilityStateEvent.EVENT_TYPE ) ) {
			handleTypedEvent( new NewBikeSharingFacilityStateEvent( event ) );
		}
	}

	private void handleTypedEvent(
			final NewBikeSharingFacilityStateEvent event) {
		try {
			final BikeSharingFacility facility = facilities.getFacilities().get( event.getFacilityId() );
			writer.newLine();
			writer.write( facility.getId()+"\t"+
					facility.getCoord().getX()+"\t"+
					facility.getCoord().getY()+"\t"+
					event.getTime()+"\t"+
					event.getNewAmountOfBikes()+"\t"+
					facility.getCapacity()+"\t"+
					getNumberOfWaitingForBikeAgent( facility.getId() )+"\t"+
					getNumberOfWaitingForSlotAgent( facility.getId() ) );
			bikesAtStation.put( facility.getId() , event.getNewAmountOfBikes() );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private int getNumberOfWaitingForBikeAgent(final Id facilityId) {
		final Integer v = waitingForBikeAgents.get( facilityId );
		return v == null ? 0 : v;
	}

	private int getNumberOfWaitingForSlotAgent(final Id facilityId) {
		final Integer v = waitingForSlotAgents.get( facilityId );
		return v == null ? 0 : v;
	}

	private int getNumberOfBikes(final Id facilityId) {
		final Integer v = bikesAtStation.get( facilityId );
		return v == null ? 0 : v;
	}

	private void handleTypedEvent(
			final AgentStopsWaitingForFreeBikeSlotEvent event) {
		try {
			final BikeSharingFacility facility = facilities.getFacilities().get( event.getFacilityId() );

			final int newAmount = getNumberOfWaitingForSlotAgent( facility.getId() ) - 1;
			waitingForSlotAgents.put( event.getFacilityId() , newAmount );

			writer.newLine();
			writer.write( facility.getId()+"\t"+
					facility.getCoord().getX()+"\t"+
					facility.getCoord().getY()+"\t"+
					event.getTime()+"\t"+
					getNumberOfBikes( facility.getId() )+"\t"+
					facility.getCapacity()+"\t"+
					getNumberOfWaitingForBikeAgent( facility.getId() )+"\t"+
					newAmount );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}	
	}

	private void handleTypedEvent(
			final AgentStartsWaitingForFreeBikeSlotEvent event) {
		try {
			final BikeSharingFacility facility = facilities.getFacilities().get( event.getFacilityId() );

			final int newAmount = getNumberOfWaitingForSlotAgent( facility.getId() ) + 1;
			waitingForSlotAgents.put( event.getFacilityId() , newAmount );

			writer.newLine();
			writer.write( facility.getId()+"\t"+
					facility.getCoord().getX()+"\t"+
					facility.getCoord().getY()+"\t"+
					event.getTime()+"\t"+
					getNumberOfBikes( facility.getId() )+"\t"+
					facility.getCapacity()+"\t"+
					getNumberOfWaitingForBikeAgent( facility.getId() )+"\t"+
					newAmount );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}	
	}

	private void handleTypedEvent(
			final AgentStopsWaitingForBikeEvent event) {
		try {
			final BikeSharingFacility facility = facilities.getFacilities().get( event.getFacilityId() );

			final int newAmount = getNumberOfWaitingForBikeAgent( facility.getId() ) - 1;
			waitingForBikeAgents.put( event.getFacilityId() , newAmount );

			writer.newLine();
			writer.write( facility.getId()+"\t"+
					facility.getCoord().getX()+"\t"+
					facility.getCoord().getY()+"\t"+
					event.getTime()+"\t"+
					getNumberOfBikes( facility.getId() )+"\t"+
					facility.getCapacity()+"\t"+
					newAmount+"\t"+
					getNumberOfWaitingForSlotAgent( facility.getId() ) );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}	
	}

	private void handleTypedEvent(
			final AgentStartsWaitingForBikeEvent event) {
		try {
			final BikeSharingFacility facility = facilities.getFacilities().get( event.getFacilityId() );

			final int newAmount = getNumberOfWaitingForBikeAgent( facility.getId() ) + 1;
			waitingForBikeAgents.put( event.getFacilityId() , newAmount );

			writer.newLine();
			writer.write( facility.getId()+"\t"+
					facility.getCoord().getX()+"\t"+
					facility.getCoord().getY()+"\t"+
					event.getTime()+"\t"+
					getNumberOfBikes( facility.getId() )+"\t"+
					facility.getCapacity()+"\t"+
					newAmount+"\t"+
					getNumberOfWaitingForSlotAgent( facility.getId() ) );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}	
	}
}

