/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.plausibility.log;


import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public abstract class LogMessage {

	protected final TransitLine transitLine;
	protected final TransitRoute transitRoute;

	public LogMessage(TransitLine transitLine, TransitRoute transitRoute) {
		this.transitLine = transitLine;
		this.transitRoute = transitRoute;
	}

	public void printLineRouteInfo(TransitLine previousLine, TransitRoute previousRoute) {
		if(((previousLine == null && previousRoute == null) || !previousLine.equals(transitLine) || !previousRoute.equals(transitRoute))) {
			System.out.println("\t" + transitLine.getId() + "\t" + transitRoute.getId());
		}
	}

	public TransitRoute getTransitRoute() {
		return transitRoute;
	}

	public TransitLine getTransitLine() {
		return transitLine;
	}
}
