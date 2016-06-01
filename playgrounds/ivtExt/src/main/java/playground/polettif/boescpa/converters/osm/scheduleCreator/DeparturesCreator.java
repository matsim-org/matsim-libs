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

package playground.polettif.boescpa.converters.osm.scheduleCreator;

import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

/**
 * The departures module interface requires as input a schedule that already contains the transit
 * stops and the route profiles. The output of this module is a schedule too. Compared to the input
 * it contains additionally the departure times. A second output of this module is a MATSim vehicle
 * file which defines all PT vehicles needed at the departures.
 *
 * @author boescpa
 */
public interface DeparturesCreator {

	void createDepartures(TransitSchedule schedule, Vehicles vehicles, String pathToInputFiles);

}
