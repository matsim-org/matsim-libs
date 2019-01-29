/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.performance.raptor;

import java.util.Map;

import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * 
 * @author aneumann
 *
 */
public class RaptorSearchData {
	// yy It is not clear to me what a "block" is in the following description.  Possibly, if we have routes a and b, and stops a1, a2, ..., b1, b2, ...,
	// then they are just consecutively put into an array, so that the next entry is either the next bus stop downstream, or the
	// starting point of the next route.  ??  kai, jun'16
	
	// Each block contains data for one route. Each route block holds sub-blocks for each departure. Each sub-block holds arrival and departure times sorted in increasing order.  
	final double[] arrivalTimes;
	final double[] departureTimes;

	// All routes each linking to its departures (stopTimes) and served stops (routeStops).
	final RouteEntry[] routes;

	// Each block holds the stops served by a route in the sequence of serving it.
	final RouteStopEntry[] routeStops;

	// Each block holds the potential transfers for each stop.
	final TransferEntry[] transfers;

	// All stops serving at least one route with links to their transfers (transfers) and routes served (stopRoutes).
	final TransitStopEntry[] stops;

	// Simple list of transit stop to index - just for speedup 
	final Map<TransitStopFacility, Integer> transitStopFacility2Index;

	public RaptorSearchData(double[] arrivalTimes, double[] departureTimes, RouteEntry[] routes, RouteStopEntry[] routeStops, TransferEntry[] transfers, TransitStopEntry[] stops, Map<TransitStopFacility, Integer> transitStopFacility2Index) {
		this.arrivalTimes = arrivalTimes;
		this.departureTimes = departureTimes;
		this.routes = routes;
		this.routeStops = routeStops;
		this.transfers = transfers;
		this.stops = stops;
		this.transitStopFacility2Index = transitStopFacility2Index;
	}

}
