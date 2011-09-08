/* *********************************************************************** *
 * project: org.matsim.*
 * JoinableTripsXmlReader.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * Reads a file with joinable trips information for later analysis.
 * @author thibautd
 */
public class JoinableTripsXmlReader extends MatsimXmlParser {

	private double distance = Double.NaN;
	private double time = Double.NaN;
	private Map<Id, JoinableTrips.TripRecord> trips =
		new HashMap<Id, JoinableTrips.TripRecord>();

	private List<JoinableTrips.JoinableTrip> currentJoinableTrips = null;
	private JoinableTrips.JoinableTrip currentJoinableTrip = null;

	public JoinableTripsXmlReader() {
		super(false);
	}

	private void reset() {
		distance = Double.NaN;
		time = Double.NaN;
		trips = new HashMap<Id, JoinableTrips.TripRecord>();
		currentJoinableTrips = null;
		currentJoinableTrip = null;
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals(JoinableTripsXmlSchemaNames.TAG) ) {
			this.distance = Double.parseDouble(atts.getValue(
					JoinableTripsXmlSchemaNames.DIST));
			this.time = Double.parseDouble(atts.getValue(
					JoinableTripsXmlSchemaNames.TIME));
		}
		else if ( name.equals(JoinableTripsXmlSchemaNames.Trip.TAG) ) {
			currentJoinableTrips = new ArrayList<JoinableTrips.JoinableTrip>();
			trips.put(
					new IdImpl(atts.getValue(JoinableTripsXmlSchemaNames.Trip.TRIP_ID)),
					new JoinableTrips.TripRecord(
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.TRIP_ID),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.AGENT_ID),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.MODE),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.ORIGIN),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.ORIGIN_ACT),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.DEPARTURE_TIME),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.DESTINATION),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.DESTINATION_ACT),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.ARRIVAL_TIME),
							atts.getValue(JoinableTripsXmlSchemaNames.Trip.LEG_NR),
							currentJoinableTrips));
		}
		else if ( name.equals(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.TAG) ) {
			currentJoinableTrip = new JoinableTrips.JoinableTrip(
					new IdImpl( atts.getValue(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.ID) ) );
			currentJoinableTrips.add(currentJoinableTrip);
		}
		else if ( name.equals(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.Passage.TAG) ) {
			currentJoinableTrip.addPassage(
					JoinableTrips.Passage.Type.valueOf(
						atts.getValue(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.Passage.TYPE)),
					Double.parseDouble(
						atts.getValue(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.Passage.DISTANCE)),
					Double.parseDouble(
						atts.getValue(JoinableTripsXmlSchemaNames.Trip.JoinableTrips.JoinableTrip.Passage.TIME)));
		}
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
	}

	public JoinableTrips getJoinableTrips() {
		return new JoinableTrips(distance, time, trips);
	}
}

