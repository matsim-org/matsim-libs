/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.hafas;

import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * The route profile module interface requires as input a schedule file that contains all transit stops.
 * As output, the module has to generate a schedule file, which contains the transit stops and the PT
 * routes with the stop sequence for each route (route profiles). These route profiles also include
 * the arrival and departure times for each stop of any route profile.
 *
 * @author boescpa
 */
public interface RouteProfileCreator {

	void createRouteProfiles(TransitSchedule schedule, String pathToInputFiles);

}
