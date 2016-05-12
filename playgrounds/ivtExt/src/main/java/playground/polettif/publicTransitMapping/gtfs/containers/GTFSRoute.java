/* *********************************************************************** *
 * project: org.matsim.*
 * Route.java
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

package playground.polettif.publicTransitMapping.gtfs.containers;

import playground.polettif.publicTransitMapping.gtfs.containers.GTFSDefinitions.RouteTypes;


import java.util.SortedMap;
import java.util.TreeMap;


public class GTFSRoute {
	                           
	//Attributes
	private String routeId;
	private String shortName;
	private RouteTypes routeType;
	private SortedMap<String, Trip> trips;

	//Methods
	/**
	 * @param shortName
	 * @param routeType
	 */
	public GTFSRoute(String routeId, String shortName, RouteTypes routeType) {
		super();
		this.routeId = routeId;
		this.shortName = shortName;
		this.routeType = routeType;
		trips = new TreeMap<>();
	}

	/**
	 * @return the routeId
	 */
	public String getRouteId() {
		return routeId;
	}

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @return the routeType
	 */
	public RouteTypes getRouteType() {
		return routeType;
	}

	/**
	 * @return the trips
	 */
	public SortedMap<String, Trip> getTrips() {
		return trips;
	}

	/**
	 * Puts a new trip
	 * @param key
	 * @param trip
	 */
	public void putTrip(String key, Trip trip) {
		trips.put(key, trip);
	}
	

}
