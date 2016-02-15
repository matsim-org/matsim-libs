/* *********************************************************************** *
 * project: org.matsim.*
 * RecordsFlatFormatWriter.java
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

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.analysis.spacetimeprismjoinabletrips.TripsPrism.PassengerRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * writes the records information in a simple flat format
 * @author thibautd
 */
public class RecordsFlatFormatWriter {
	private static final Logger log =
		Logger.getLogger(RecordsFlatFormatWriter.class);

	private final BufferedWriter writer;
	private final Counter counter = new Counter( "writing passenger record # " );

	public RecordsFlatFormatWriter(final String outFile) {
		try {
			this.writer = IOUtils.getBufferedWriter( outFile );

			log.info( "opening "+outFile+":" );
			writer.write( "driverRecordId\t"
					+"passengerRecordId\t"
					+"directDriverDist\t"
					+"directDriverDur\t"
					+"accessDriverDist\t"
					+"jointDriverDist\t"
					+"egressDriverDist\t"
					+"accessDriverDur\t"
					+"jointDriverDur\t"
					+"egressDriverDur\t"
					+"minTimeWindow" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public void close() {
		try {
			counter.printCounter();
			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public void writePassengerTrip(
			final PassengerRecord passengerTrip) {
		try {
			writer.newLine();
			counter.incCounter();
			writer.write(
					passengerTrip.getDriverRecord().getTripId()+"\t"+
					passengerTrip.getPassengerRecord().getTripId()+"\t"+
					passengerTrip.getDirectDriverDist()+"\t"+
					passengerTrip.getDirectDriverFreeFlowDur()+"\t"+
					passengerTrip.getDriverAccessDist()+"\t"+
					passengerTrip.getDriverJointDist()+"\t"+
					passengerTrip.getDriverEgressDist()+"\t"+
					passengerTrip.getDriverAccessDur()+"\t"+
					passengerTrip.getDriverJointDur()+"\t"+
					passengerTrip.getDriverEgressDur()+"\t"+
					passengerTrip.getMinTimeWindow());
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public static void writeRecords(final List<Record> records, final String file) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter( file );

			log.info( "writing "+file+":" );
			Counter counter = new Counter( "writing record # " );
			writer.write( "recordId\tagentId\ttripNr\tmode\torigin\tdestination\tdepartureTime\tarrivalTime" );

			for (Record r : records) {
				counter.incCounter();
				writer.newLine();
				writer.write(
						r.getTripId()+"\t"+
						r.getAgentId()+"\t"+
						r.getNumberOfTripInAgentPlan()+"\t"+
						r.getTripMode()+"\t"+
						r.getOriginLink()+"\t"+
						r.getDestinationLink()+"\t"+
						r.getDepartureTime()+"\t"+
						r.getArrivalTime());
			}
			counter.printCounter();

			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}

