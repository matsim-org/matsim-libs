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

package playground.polettif.publicTransitMapping.workbench;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

public class CutSchedule {
	
	public static void main(final String[] args) {
		cutScheduleToZurich("mts/fromHafas/ch.xml.gz", "vehicles/ch_hafas_vehicles.xml.gz", "mts/fromHafas/test.xml.gz", "vehicles/test.xml.gz");
//		TransitSchedule schedule = ScheduleTools.readTransitSchedule("mts/fromHafas/ch.xml.gz");

//		ScheduleCleaner.cutSchedule(schedule, Collections.singleton(Id.create(8503000, TransitStopFacility.class)));

//		ScheduleTools.writeTransitSchedule(schedule, "mts/fromHafas/test.xml.gz");
	}

	private static void cutScheduleToZurich(String scheduleFile, String vehiclesFile, String outputSchedule, String outputVehicles) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(scheduleFile);
		Vehicles vehicles = ScheduleTools.readVehicles(vehiclesFile);

//		Coord city = new Coord(2683518.0, 1246836.0);
		Coord effretikon = new Coord(2693780.0, 1253409.0);
		double radius = 5000;

		ScheduleCleaner.cutSchedule(schedule, effretikon, radius);
		ScheduleCleaner.cleanVehicles(schedule, vehicles);

		ScheduleTools.writeTransitSchedule(schedule, outputSchedule);
		ScheduleTools.writeVehicles(vehicles, outputVehicles);
	}
}