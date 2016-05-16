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

import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.publicTransitMapping.gtfs.GTFSReader;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

public class RunGTFS2UnmappedMTS {
	
	public static void main(final String[] args) {
		String base = "C:/Users/Flavio/Desktop/data/";

		String gtfsPath = base+"gtfs/zvv/";
		String mtsFile = base+"mts/fromGtfs/zvv_mostServices.xml";
		String vehiclesFile = base+"vehicles/fromGtfs/zvv_mostServices_vehicles.xml";
		String shapeFile = base+"gtfs/shp/zvv_mostServices.shp";

		// Load Schedule
		GTFSReader gtfsReader = new GTFSReader(gtfsPath, GTFSReader.DAY_WITH_MOST_SERVICES, "CH1903_LV03_Plus");
		gtfsReader.writeShapeFile(shapeFile);

		TransitSchedule schedule = gtfsReader.getSchedule();
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
		ScheduleTools.writeTransitSchedule(schedule, mtsFile);

		ScheduleTools.writeVehicles(gtfsReader.getVehicles(), vehiclesFile);
	}
}