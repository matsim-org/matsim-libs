/* *********************************************************************** *
 * project: org.matsim.*
 * WaitTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,  *
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

package org.matsim.contrib.eventsBasedPTRouter.waitTimes;

import java.io.Serializable;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Gives an average of the wait time of people for a line, route, stop and in a time of the day 
 * 
 * @author sergioo
 */

public interface WaitTime extends Serializable{

	//Methods
	public double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<org.matsim.facilities.Facility> stopId, double time);

}
