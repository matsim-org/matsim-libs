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
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips;

/**
 * @author thibautd
 */
public class JoinableTripsXmlWriter extends MatsimXmlWriter {
	private final JoinableTrips toWrite;

	private final Counter count = new Counter("Dumping trip #");
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
				JoinableTripsXmlSchemaNames.ROOT_TAG,
				null);
		this.writeConditions();
		this.writeTrips();
		this.writeEndTag(JoinableTripsXmlSchemaNames.ROOT_TAG);
	}

	private void writeConditions() {
		this.writeStartTag(JoinableTripsXmlSchemaNames.CONDITIONS_TAG, null);

		for (AcceptabilityCondition condition : toWrite.getConditions()) {
			this.writeStartTag(
					JoinableTripsXmlSchemaNames.CONDITION_TAG,
					getAttributes(condition),
					true);
		}

		this.writeEndTag(JoinableTripsXmlSchemaNames.CONDITIONS_TAG);
	}

	private List<Tuple<String, String>> getAttributes(
			final AcceptabilityCondition condition) {
		List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();

		atts.add(createTuple(JoinableTripsXmlSchemaNames.TIME, condition.getTime()));
		atts.add(createTuple(JoinableTripsXmlSchemaNames.DIST, condition.getDistance()));

		return atts;
	}

	private void writeTrips() {
		Collection<JoinableTrips.TripRecord> records = toWrite.getTripRecords().values();

		for ( JoinableTrips.TripRecord record : records ) {
			count.incCounter();
			this.writeStartTag(
					JoinableTripsXmlSchemaNames.TRIP_TAG,
					getAttributes(record));
			this.writeJoinableTrips(record);
			this.writeEndTag(JoinableTripsXmlSchemaNames.TRIP_TAG);
		}
	}

	private List<Tuple<String, String>> getAttributes(
			final JoinableTrips.TripRecord record) {
		List<Tuple<String, String>> output = new ArrayList<Tuple<String, String>>();

		output.add(createTuple(
					JoinableTripsXmlSchemaNames.TRIP_ID,
					record.getId().toString()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.AGENT_ID,
					record.getAgentId().toString()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.MODE,
					record.getMode()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.ORIGIN,
					record.getOriginLinkId().toString()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.ORIGIN_ACT,
					record.getOriginActivityType()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.DESTINATION,
					record.getDestinationLinkId().toString()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.DESTINATION_ACT,
					record.getDestinationActivityType()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.DEPARTURE_TIME,
					""+record.getDepartureTime()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.ARRIVAL_TIME,
					""+record.getArrivalTime()));
		output.add(createTuple(
					JoinableTripsXmlSchemaNames.LEG_NR,
					""+record.getLegNumber()));

		return output;
	}

	private void writeJoinableTrips(final JoinableTrips.TripRecord record) {
		List<JoinableTrips.JoinableTrip> joinableTrips = record.getJoinableTrips();

		for (JoinableTrips.JoinableTrip trip : joinableTrips) {
			this.writeStartTag(
					JoinableTripsXmlSchemaNames.JOINABLE_TAG,
					getAttributes(trip));

			this.writeFullfilledConditions(trip);

			this.writeEndTag(JoinableTripsXmlSchemaNames.JOINABLE_TAG);
		}
	}

	private List<Tuple<String, String>> getAttributes(
			final JoinableTrips.JoinableTrip trip) {
		List<Tuple<String, String>> output = new ArrayList<Tuple<String, String>>();

		output.add(createTuple(
					JoinableTripsXmlSchemaNames.TRIP_ID,
					trip.getTripId().toString()));

		return output;
	}

	private void writeFullfilledConditions(final JoinableTrips.JoinableTrip trip) {
		List<AcceptabilityCondition> conditions = trip.getFullfilledConditions();

		for ( AcceptabilityCondition condition : conditions ) {
			this.writeStartTag(
					JoinableTripsXmlSchemaNames.CONDITION_TAG,
					getAttributes(condition),
					true);
		}
	}
}

