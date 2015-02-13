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
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * @author aneumann
 *
 */
public class RouteSegment {
	
	final TransitStopFacility fromStop;
	final TransitStopFacility toStop;
	final double travelTime;
	final Id<TransitLine> lineTaken;
	final Id<TransitRoute> routeTaken;
	
	public RouteSegment(TransitStopFacility fromStop, TransitStopFacility toStop, double travelTime, Id<TransitLine> lineTaken, Id<TransitRoute> routeTaken) {
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.travelTime = travelTime;
		this.lineTaken = lineTaken;
		this.routeTaken = routeTaken;
	}
	
	@Override
	public String toString() {
		return "From: " + fromStop.getId() + " to " + toStop.getId() + " in " + travelTime + "s via " + routeTaken;
	}

}
