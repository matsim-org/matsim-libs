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
//		final String gtfsPath = "C:/Users/polettif/Desktop/data/gtfs/zvv/";
		final String gtfsPath = "C:/Users/polettif/Desktop/data/gtfs/zvv/";
		final String mtsFile = "C:/Users/polettif/Desktop/data/mts/unmapped/fromGtfs/zvv_mostServices.xml";
		final String shapeFile = "C:/Users/polettif/Desktop/data/gtfs/shp/zvv_mostServices.shp";
//		final String mtsFile = "C:/Users/polettif/Desktop/output/gtfs2mts/zvv_unmappedSchedule.xml";
//		final String mtsFile = "C:/Users/polettif/Desktop/output/gtfs2mts/google_sample.xml";

		// Load Schedule
		GTFSReader gtfsReader = new GTFSReader(gtfsPath, GTFSReader.DAY_WITH_MOST_SERVICES, "CH1903_LV03_Plus");
		gtfsReader.writeShapeFile(shapeFile);
		TransitSchedule schedule = gtfsReader.getSchedule();
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
		ScheduleTools.writeTransitSchedule(schedule, mtsFile);
	}
	
}