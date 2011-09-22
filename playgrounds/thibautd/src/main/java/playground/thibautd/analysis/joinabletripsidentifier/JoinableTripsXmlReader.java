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
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

/**
 * Reads a file with joinable trips information for later analysis.
 * @author thibautd
 */
public class JoinableTripsXmlReader extends MatsimXmlParser {

	private List<AcceptabilityCondition> conditions =
		new ArrayList<AcceptabilityCondition>();
	private Map<Id, JoinableTrips.TripRecord> trips =
		new HashMap<Id, JoinableTrips.TripRecord>();

	private List<JoinableTrips.JoinableTrip> currentJoinableTrips = null;
	private JoinableTrips.JoinableTrip currentJoinableTrip = null;
	private Id currentPassengerTrip = null;

	private final Counter count = new Counter("Import of trip # ");

	public JoinableTripsXmlReader() {
		super(false);
	}

	private void reset() {
		count.reset();
		conditions = new ArrayList<AcceptabilityCondition>();
		trips = new HashMap<Id, JoinableTrips.TripRecord>();
		currentJoinableTrips = null;
		currentJoinableTrip = null;
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals(JoinableTripsXmlSchemaNames.CONDITION_TAG) ) {
			if (context.peek().equals(JoinableTripsXmlSchemaNames.CONDITIONS_TAG)) {
				conditions.add(getCondition(atts));
			}
			else {
				currentJoinableTrip.getFullfilledConditions().add(getCondition(atts));
			}
		}
		else if ( name.equals(JoinableTripsXmlSchemaNames.TRIP_TAG) ) {
			count.incCounter();
			currentJoinableTrips = new ArrayList<JoinableTrips.JoinableTrip>();
			currentPassengerTrip = IdPool.getId(atts.getValue(JoinableTripsXmlSchemaNames.TRIP_ID));
			trips.put(
					currentPassengerTrip,
					new JoinableTrips.TripRecord(
							IdPool.getId(atts.getValue(JoinableTripsXmlSchemaNames.TRIP_ID)),
							IdPool.getId(atts.getValue(JoinableTripsXmlSchemaNames.AGENT_ID)),
							atts.getValue(JoinableTripsXmlSchemaNames.MODE),
							IdPool.getId(atts.getValue(JoinableTripsXmlSchemaNames.ORIGIN)),
							atts.getValue(JoinableTripsXmlSchemaNames.ORIGIN_ACT),
							atts.getValue(JoinableTripsXmlSchemaNames.DEPARTURE_TIME),
							IdPool.getId(atts.getValue(JoinableTripsXmlSchemaNames.DESTINATION)),
							atts.getValue(JoinableTripsXmlSchemaNames.DESTINATION_ACT),
							atts.getValue(JoinableTripsXmlSchemaNames.ARRIVAL_TIME),
							atts.getValue(JoinableTripsXmlSchemaNames.LEG_NR),
							currentJoinableTrips));
		}
		else if ( name.equals(JoinableTripsXmlSchemaNames.JOINABLE_TAG) ) {
			currentJoinableTrip = new JoinableTrips.JoinableTrip(
					currentPassengerTrip,
					IdPool.getId( atts.getValue(JoinableTripsXmlSchemaNames.TRIP_ID) ) );
			currentJoinableTrips.add(currentJoinableTrip);
		}
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
	}

	private AcceptabilityCondition getCondition(final Attributes atts) {
		AcceptabilityCondition condition = new AcceptabilityCondition(
				Double.parseDouble(atts.getValue(JoinableTripsXmlSchemaNames.DIST)),
				Double.parseDouble(atts.getValue(JoinableTripsXmlSchemaNames.TIME)));

		int i = conditions.indexOf(condition);
		return i == -1 ? condition : conditions.get(i);
	}

	public JoinableTrips getJoinableTrips() {
		return new JoinableTrips(conditions, trips);
	}
}

class IdPool {
	private static Map<String, Id> map = new HashMap<String, Id>();

	public static Id getId(final String string) {
		Id id = map.get(string);

		if (id == null) {
			id = new IdImpl(string);
			map.put(string, id);
		}

		return id;
	}
}
