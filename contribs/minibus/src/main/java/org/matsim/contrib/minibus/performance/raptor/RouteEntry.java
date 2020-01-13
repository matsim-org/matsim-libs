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

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * 
 * @author aneumann
 *
 */
public class RouteEntry {
	
	final Id<TransitLine> lineId;
	final Id<TransitRoute> routeId;

	final int indexOfFirstDeparture;
	final int numberOfDepartures;

	final int indexOfFirstStop;
	final int numberOfRouteStops;
	

	
	public RouteEntry(Id<TransitLine> lineId, Id<TransitRoute> routeId, int indexOfFirstDeparture, int numberOfDepartures, int indexOfFirstStop, int numberOfRouteStops) {
		this.lineId = lineId;
		this.routeId = routeId;
		
		this.indexOfFirstDeparture = indexOfFirstDeparture;
		this.numberOfDepartures = numberOfDepartures;
		
		this.indexOfFirstStop = indexOfFirstStop;
		this.numberOfRouteStops = numberOfRouteStops;
	}
	
	@Override
	public String toString() {
		return lineId + ", " + routeId + ", " + numberOfRouteStops + " from " + indexOfFirstStop + ", " + numberOfDepartures + " dep from " + indexOfFirstDeparture;
	}
}
