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

package playground.polettif.publicTransitMapping.tools;

import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class ExtractDebugSchedule {

	/**
	 * Extracts one route from a schedule and writes it
	 * to a new file.
	 * @param args [0] schedule file
	 *             [1] transit route id
	 *             [2] output debug schedule file
	 */
	public static void main(final String[] args) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(args[0]);
		TransitSchedule debug = ScheduleTools.createSchedule();

		for(TransitLine tl : schedule.getTransitLines().values()) {
			for(TransitRoute tr : tl.getRoutes().values()) {
				if(tr.getId().toString().equals(args[1])) {
					TransitLine line = debug.getFactory().createTransitLine(tl.getId());
					line.addRoute(tr);

					for(TransitRouteStop rs : tr.getStops()) {
						debug.addStopFacility(rs.getStopFacility());
					}
				}
			}
		}

		ScheduleTools.writeTransitSchedule(debug, args[2]);
	}
	
}