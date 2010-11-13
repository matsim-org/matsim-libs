/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleConstants.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule;

/**
 * Commonly used constants in readers and writers for xml-based
 * transitSchedule file formats.
 *
 * @author mrieser
 */
/*package*/ abstract class Constants {
	/*package*/ static final String TRANSIT_STOPS = "transitStops";
	/*package*/ static final String STOP_FACILITY = "stopFacility";
	/*package*/ static final String TRANSIT_SCHEDULE = "transitSchedule";
	/*package*/ static final String TRANSIT_LINE = "transitLine";
	/*package*/ static final String TRANSIT_ROUTE = "transitRoute";
	/*package*/ static final String DESCRIPTION = "description";
	/*package*/ static final String ROUTE_PROFILE = "routeProfile";
	/*package*/ static final String STOP = "stop";
	/*package*/ static final String ROUTE = "route";
	/*package*/ static final String LINK = "link";
	/*package*/ static final String DEPARTURES = "departures";
	/*package*/ static final String DEPARTURE = "departure";
	/*package*/ static final String ID = "id";
	/*package*/ static final String X = "x";
	/*package*/ static final String Y = "y";
	/*package*/ static final String NAME = "name";
	/*package*/ static final String REF_ID = "refId";
	/*package*/ static final String LINK_REF_ID = "linkRefId";
	/*package*/ static final String TRANSPORT_MODE = "transportMode";
	/*package*/ static final String DEPARTURE_TIME = "departureTime";
	/*package*/ static final String VEHICLE_REF_ID = "vehicleRefId";
	/*package*/ static final String DEPARTURE_OFFSET = "departureOffset";
	/*package*/ static final String ARRIVAL_OFFSET = "arrivalOffset";
	/*package*/ static final String AWAIT_DEPARTURE = "awaitDeparture";
	/*package*/ static final String IS_BLOCKING = "isBlocking";

}
