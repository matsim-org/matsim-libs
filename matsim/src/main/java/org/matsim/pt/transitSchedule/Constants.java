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
abstract class Constants {
	static final String TRANSIT_STOPS = "transitStops";
	static final String STOP_FACILITY = "stopFacility";
	static final String TRANSIT_SCHEDULE = "transitSchedule";
	static final String TRANSIT_LINE = "transitLine";
	static final String TRANSIT_ROUTE = "transitRoute";
	static final String DESCRIPTION = "description";
	static final String ROUTE_PROFILE = "routeProfile";
	static final String STOP = "stop";
	static final String ROUTE = "route";
	static final String LINK = "link";
	static final String DEPARTURES = "departures";
	static final String DEPARTURE = "departure";
	static final String ID = "id";
	static final String X = "x";
	static final String Y = "y";
	static final String Z = "z";
	static final String NAME = "name";
	static final String REF_ID = "refId";
	static final String LINK_REF_ID = "linkRefId";
	static final String TRANSPORT_MODE = "transportMode";
	static final String DEPARTURE_TIME = "departureTime";
	static final String VEHICLE_REF_ID = "vehicleRefId";
	static final String DEPARTURE_OFFSET = "departureOffset";
	static final String ARRIVAL_OFFSET = "arrivalOffset";
	static final String ALLOW_BOARDING = "allowBoarding";
	static final String ALLOW_ALIGHTING = "allowAlighting";
	static final String AWAIT_DEPARTURE = "awaitDeparture";
	static final String IS_BLOCKING = "isBlocking";
	static final String STOP_AREA_ID = "stopAreaId";
	static final String ATTRIBUTES = "attributes";
	static final String ATTRIBUTE = "attribute";
	static final String MINIMAL_TRANSFER_TIMES = "minimalTransferTimes";
	static final String RELATION = "relation";
	static final String FROM_STOP = "fromStop";
	static final String TO_STOP = "toStop";
	static final String TRANSFER_TIME = "transferTime";
	static final String CHAINED_DEPARTURE = "chainedDeparture";
	static final String TO_DEPARTURE = "toDeparture";
	static final String TO_TRANSIT_LINE = "toTransitLine";
	static final String TO_TRANSIT_ROUTE = "toTransitRoute";
}
