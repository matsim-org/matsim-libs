/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.router.connectionscanalgorithm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author thibautd
 */
public class ContigousConnections {
	// idea frombhttp://stackoverflow.com/a/9632711
	// plan: first implement ByteBuffer version, then extract interface, implement naive version, and check comparative perf

	// OFFSETS
	private static final int ARRIVALTIME_OFFSET = 0;
	private static final int ORIGINID_OFFSET = ARRIVALTIME_OFFSET + 8;
	private static final int DESTINATIONID_OFFSET = ORIGINID_OFFSET + 4;
	private static final int TRIPID_OFFSET = DESTINATIONID_OFFSET + 4;
	private static final int DEPARTURETIME_OFFSET = TRIPID_OFFSET + 4;
	private static final int EARLIESTARRIVAL_OFFSET = DEPARTURETIME_OFFSET + 8;
	private static final int LENGTH = EARLIESTARRIVAL_OFFSET + 8;

	private final ByteBuffer buffer;

	public ContigousConnections(final int nConnections) {
		this( ByteBuffer.allocateDirect( nConnections * LENGTH ).order( ByteOrder.nativeOrder() ) );
	}

	ContigousConnections( final ByteBuffer buffer ) {
		this.buffer = buffer;
	}

	private static int pointer( int index ) {
		if ( index < 0 ) throw new IllegalArgumentException( "negative index: "+index );
		return index * LENGTH;
	}

	public void setConnection(
			final int connection,
			final int originId,
			final int destinationId,
			final int tripId,
			final double departureTime,
			final double arrivalTime ) {
		setOriginId( connection , originId );
		setDestinationId( connection , destinationId );
		setTripId( connection , tripId );
		setDepartureTime( connection , departureTime );
		setArrivalTime( connection , arrivalTime );
	}

	public int getOriginId( final int connection ) {
		return buffer.getInt( pointer( connection ) + ORIGINID_OFFSET );
	}

	public void setOriginId( final int connection , final int originId) {
		buffer.putInt( pointer( connection ) + ORIGINID_OFFSET , originId );
	}

	public int getDestinationId( final int connection ) {
		return buffer.getInt( pointer( connection ) + DESTINATIONID_OFFSET );
	}

	public void setDestinationId( final int connection , final int destinationId) {
		buffer.putInt( pointer( connection ) + DESTINATIONID_OFFSET , destinationId );
	}

	public int getTripId( final int connection ) {
		return buffer.getInt( pointer( connection ) + TRIPID_OFFSET );
	}

	public void setTripId( final int connection , final int tripId) {
		buffer.putInt( pointer( connection ) + TRIPID_OFFSET , tripId );
	}

	public double getDepartureTime( final int connection ) {
		return buffer.getDouble( pointer( connection ) + DEPARTURETIME_OFFSET );
	}

	public void setDepartureTime( final int connection , final double departureTime) {
		buffer.putDouble( pointer( connection ) + DEPARTURETIME_OFFSET , departureTime );
	}

	public double getArrivalTime( final int connection ) {
		return buffer.getDouble( pointer( connection ) + ARRIVALTIME_OFFSET );
	}

	public void setArrivalTime( final int connection , final double arrivalTime) {
		buffer.putDouble( pointer( connection ) + ARRIVALTIME_OFFSET , arrivalTime );
	}

	public double getEarliestArrival( final int connection ) {
		return buffer.getDouble( pointer( connection ) + EARLIESTARRIVAL_OFFSET );
	}

	public void setEarliestArrival( final int connection , final double arrivalTime) {
		buffer.putDouble( pointer( connection ) + EARLIESTARRIVAL_OFFSET , arrivalTime );
	}
}
