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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Reads a file with joinable trips information for later analysis.
 * @author thibautd
 */
public class JoinableTripsXmlReader extends MatsimXmlParser {

	private final Map<String, AcceptabilityCondition> conditions = new HashMap<String, AcceptabilityCondition>();
	//private List<AcceptabilityCondition> conditions =
	//	new ArrayList<AcceptabilityCondition>();
	private final Map<Id, JoinableTrips.TripRecord> trips =
		new HashMap<Id, JoinableTrips.TripRecord>();

	private List<JoinableTrips.JoinableTrip> currentJoinableTrips = null;
	private JoinableTrips.JoinableTrip currentJoinableTrip = null;
	private Id currentPassengerTrip = null;
	private AcceptabilityCondition currentCondition = null;
	private TripInfo currentTripInfo = null;

	private final Counter count = new Counter("Import of trip # ");

	public JoinableTripsXmlReader() {
		super(false);
	}

	@Override
	public void startTag(
			final String name,
			final Attributes atts,
			final Stack<String> context) {
		if ( name.equals(JoinableTripsXmlSchemaNames.CONDITION_TAG) ) {
			//if (context.peek().equals(JoinableTripsXmlSchemaNames.CONDITIONS_TAG)) {
			//	conditions.put(getCondition(atts));
			//}
			//else {
				currentCondition = getCondition(atts);
			//}
		}
		else if ( name.equals(JoinableTripsXmlSchemaNames.TRIP_TAG) ) {
			count.incCounter();
			currentJoinableTrips = new ArrayList<JoinableTrips.JoinableTrip>();
			currentPassengerTrip = Id.create(atts.getValue(JoinableTripsXmlSchemaNames.TRIP_ID), Trip.class );
			trips.put(
					currentPassengerTrip,
					new JoinableTrips.TripRecord(
							Id.create(atts.getValue(JoinableTripsXmlSchemaNames.TRIP_ID), Trip.class),
							Id.create(atts.getValue(JoinableTripsXmlSchemaNames.AGENT_ID), Person.class),
							atts.getValue(JoinableTripsXmlSchemaNames.MODE),
							Id.create(atts.getValue(JoinableTripsXmlSchemaNames.ORIGIN), Link.class),
							atts.getValue(JoinableTripsXmlSchemaNames.ORIGIN_ACT),
							atts.getValue(JoinableTripsXmlSchemaNames.DEPARTURE_TIME),
							Id.create(atts.getValue(JoinableTripsXmlSchemaNames.DESTINATION), Link.class),
							atts.getValue(JoinableTripsXmlSchemaNames.DESTINATION_ACT),
							atts.getValue(JoinableTripsXmlSchemaNames.ARRIVAL_TIME),
							atts.getValue(JoinableTripsXmlSchemaNames.LEG_NR),
							currentJoinableTrips));
		}
		else if ( name.equals(JoinableTripsXmlSchemaNames.JOINABLE_TAG) ) {
			currentJoinableTrip = new JoinableTrips.JoinableTrip(
					currentPassengerTrip,
					Id.create( atts.getValue(JoinableTripsXmlSchemaNames.TRIP_ID) , Trip.class ) );
			currentJoinableTrips.add(currentJoinableTrip);
		}
		else if ( name.equals( JoinableTripsXmlSchemaNames.FULLFILLED_CONDITION_TAG ) ) {
			currentTripInfo = getTripInfo( atts );
		}
	}

	@Override
	public void endTag(
			final String name,
			final String content,
			final Stack<String> context) {
		if ( name.equals( JoinableTripsXmlSchemaNames.FULLFILLED_CONDITION_TAG ) ) {
			currentJoinableTrip.getFullfilledConditionsInfo().put( currentCondition , currentTripInfo );
			currentCondition = null;
			currentTripInfo = null;
		}
	}

	private AcceptabilityCondition getCondition(final Attributes atts) {
		final String d = atts.getValue(JoinableTripsXmlSchemaNames.DIST);
		final String t = atts.getValue(JoinableTripsXmlSchemaNames.TIME);
		final String key = d+"_"+t;

		AcceptabilityCondition condition = conditions.get( key );

		if (condition == null) {
			condition = new AcceptabilityCondition(
					(int) Math.round( Double.parseDouble( d ) ),
					(int) Math.round( Double.parseDouble( t ) ));
			conditions.put( key , condition );
		}

		return condition;
	}

	private static TripInfo getTripInfo(final Attributes atts) {
		return new TripInfo(
				Double.parseDouble( atts.getValue(JoinableTripsXmlSchemaNames.PU_WALK_DIST) ),
				Double.parseDouble( atts.getValue(JoinableTripsXmlSchemaNames.DO_WALK_DIST) ) );
	}

	public JoinableTrips getJoinableTrips() {
		return new JoinableTrips(new ArrayList<AcceptabilityCondition>(conditions.values()), trips);
	}
}
