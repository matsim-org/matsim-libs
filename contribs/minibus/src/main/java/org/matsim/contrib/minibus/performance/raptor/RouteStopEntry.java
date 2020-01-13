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


/**
 * 
 * @author aneumann
 *
 */
public class RouteStopEntry {

	final int indexOfRoute;
	final int indexOfStopFacility;
	
	final int indexOfRouteStop;
	final int numberOfRemainingStopsInThisRoute;
	
	RouteStopEntry(int indexOfRoute, int indexOfStopFacility, int indexOfRouteStop, int numberOfRemainingStopsInThisRoute) {
		this.indexOfRoute = indexOfRoute;
		this.indexOfStopFacility = indexOfStopFacility;
		this.indexOfRouteStop = indexOfRouteStop;
		this.numberOfRemainingStopsInThisRoute = numberOfRemainingStopsInThisRoute;
	}

	@Override
	public String toString() {
		return "Id " + indexOfRouteStop + ", " + this.numberOfRemainingStopsInThisRoute + " to go, Id stop " + this.indexOfStopFacility;
	}
	
}
