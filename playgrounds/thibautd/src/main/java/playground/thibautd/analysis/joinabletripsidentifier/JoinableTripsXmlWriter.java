/* *********************************************************************** *
 * project: org.matsim.*
 * JoinableTripsXmlWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.joinabletripsidentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips;

/**
 * @author thibautd
 */
public class JoinableTripsXmlWriter extends MatsimXmlWriter {
	private final JoinableTrips toWrite;

	// /////////////////////////////////////////////////////////////////////////
	// Constructor
	// /////////////////////////////////////////////////////////////////////////
	public JoinableTripsXmlWriter(
			final JoinableTrips toWrite) {
		this.toWrite = toWrite;
	}

	// /////////////////////////////////////////////////////////////////////////
	// writing methods
	// /////////////////////////////////////////////////////////////////////////
	public void write(final String fileName) throws UncheckedIOException {
		this.openFile(fileName);
		this.write();
		this.close();
	}

	private void write() {
		this.writeXmlHead();
		this.writeStartTag(
				JoinableTripsXmlSchemaNames.TAG,
				getGlobalAttributes());
		this.writeTrips();
		this.writeEndTag(JoinableTripsXmlSchemaNames.TAG);
	}

	private List<Tuple<String, String>> getGlobalAttributes() {
		List<Tuple<String, String>> output = new ArrayList<Tuple<String, String>>();

		output.add(createTuple(
					JoinableTripsXmlSchemaNames.DIST,
					toWrite.getDistanceRadius()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.TIME,
					toWrite.getAcceptableTimeDifference()));

		return output;
	}

	private void writeTrips() {
		Collection<JoinableTrips.TripRecord> records = toWrite.getTripRecords().values();

		for ( JoinableTrips.TripRecord record : records ) {
			this.writeStartTag(
					JoinableTripsXmlSchemaNames.Trip.TAG,
					getAttributes(record));
			this.writeJoinableTrips(record);
			this.writeEndTag(JoinableTripsXmlSchemaNames.Trip.TAG);
		}
	}

	private List<Tuple<String, String>> getAttributes(
			final JoinableTrips.TripRecord record) {
		List<Tuple<String, String>> output = new ArrayList<Tuple<String, String>>();

		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.TRIP_ID,
					record.getId().toString()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.AGENT_ID,
					record.getAgentId().toString()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.MODE,
					record.getMode()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.ORIGIN,
					record.getOriginLinkId().toString()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.ORIGIN_ACT,
					record.getOriginActivityType()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.DESTINATION,
					record.getDestinationLinkId().toString()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.DESTINATION_ACT,
					record.getDestinationActivityType()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.DEPARTURE_TIME,
					""+record.getDepartureTime()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.ARRIVAL_TIME,
					""+record.getArrivalTime()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.LEG_NR,
					""+record.getLegNumber()));

		return output;
	}

	private void writeJoinableTrips(final JoinableTrips.TripRecord record) {
		List<JoinableTrips.JoinableTrip> joinableTrips = record.getJoinableTrips();

		this.writeStartTag(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.TAG, null);

		for (JoinableTrips.JoinableTrip trip : joinableTrips) {
			this.writeStartTag(
					JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.TAG,
					getAttributes(trip));

			this.writePassages(trip);

			this.writeEndTag(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.TAG);
		}

		this.writeEndTag(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.TAG);
	}

	private List<Tuple<String, String>> getAttributes(
			final JoinableTrips.JoinableTrip trip) {
		List<Tuple<String, String>> output = new ArrayList<Tuple<String, String>>();

		output.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.ID,
					trip.getTripId().toString()));

		return output;
	}

	private void writePassages(final JoinableTrips.JoinableTrip trip) {
		List<JoinableTrips.Passage> passages = trip.getPassages();

		for ( JoinableTrips.Passage passage : passages ) {
			List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>();

			attributes.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.Passage.TYPE,
					passage.getType().toString()));
			attributes.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.Passage.DISTANCE,
					""+passage.getDistance()));
			attributes.add(createTuple(
					JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.Passage.TIME,
					""+passage.getTimeDifference()));

			this.writeStartTag(
					JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.Passage.TAG,
					attributes,
					true); // close
		}
	}
}

