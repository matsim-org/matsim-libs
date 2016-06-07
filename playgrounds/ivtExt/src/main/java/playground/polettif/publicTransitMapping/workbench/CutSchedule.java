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
import playground.polettif.boescpa.lib.tools.coordUtils.CoordFilter;
import playground.polettif.boescpa.lib.tools.spatialCutting.ScheduleCutter;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

public class CutSchedule {
	
	public static void main(final String[] args) {
		cutScheduleToZurich("mts/fromHafas/ch.xml.gz", "vehicles/ch_vehicles.xml.gz", "mts/fromHafas/zurich.xml.gz", "vehicles/zurich_vehicles.xml.gz");
	}

	private static void cutScheduleToZurich(String scheduleFile, String vehiclesFile, String outputSchedule, String outputVehicles) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(scheduleFile);
		Vehicles vehicles = ScheduleTools.readVehicles(vehiclesFile);

//		Coord city = new Coord(2683518.0, 1246836.0);
		Coord effretikon = new Coord(2693780.0, 1253409.0);
		double radius = 8000;

		new ScheduleCutter(schedule, vehicles, new CoordFilter.CoordFilterCircle(effretikon, radius)).cutSchedule();

		ScheduleCleaner.cleanVehicles(schedule, vehicles);

		ScheduleTools.writeTransitSchedule(schedule, outputSchedule);
		ScheduleTools.writeVehicles(vehicles, outputVehicles);
	}
}