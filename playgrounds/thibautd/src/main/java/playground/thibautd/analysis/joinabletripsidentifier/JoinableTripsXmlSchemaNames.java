/* *********************************************************************** *
 * project: org.matsim.*
 * JoinableTripsXmlSchemaNames.java
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

/**
 * Defines tags for XML writing of joinable trips
 * @author thibautd
 */
public class JoinableTripsXmlSchemaNames {
	// do not instanciate
	private JoinableTripsXmlSchemaNames() {}

	public static final String ROOT_TAG = "tripInformation";

	public static final String CONDITIONS_TAG = "conditions";
	public static final String CONDITION_TAG = "condition";

	public static final String TRIPS_TAG = "trips";

	public static final String TIME = "acceptableTimeDifference";
	public static final String DIST = "acceptableDistance";

	public static final String TRIP_TAG = "trip";

	public static final String TRIP_ID = "id";
	public static final String AGENT_ID = "agentId";
	public static final String MODE = "mode";
	public static final String ORIGIN = "originLinkId";
	public static final String ORIGIN_ACT = "originActivityType";
	public static final String DESTINATION = "destinationLinkId";
	public static final String DESTINATION_ACT = "destinationActivityType";
	public static final String DEPARTURE_TIME = "departureTime";
	public static final String ARRIVAL_TIME = "arrivalTime";
	public static final String LEG_NR = "legNumber";

	public static final String JOINABLE_TAG = "joinableTrip";
}

