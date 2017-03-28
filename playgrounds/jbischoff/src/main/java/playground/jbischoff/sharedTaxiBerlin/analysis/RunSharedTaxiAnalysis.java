/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.sharedTaxiBerlin.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.michalm.drt.analysis.VehicleOccupancyEvaluator;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunSharedTaxiAnalysis {
public static void main(String[] args) {
	String eventsFile = "C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/output_events.xml.gz";
	
	EventsManager events = EventsUtils.createEventsManager();
	VehicleOccupancyEvaluator vehicleOccupancyEvaluator = new VehicleOccupancyEvaluator(16*3600, 32*3600, 4);
	events.addHandler(vehicleOccupancyEvaluator);
	new MatsimEventsReader(events).readFile(eventsFile);
//	vehicleOccupancyEvaluator.writeDetailedOccupancyFiles("C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/vehicles/");
	vehicleOccupancyEvaluator.calcAndWriteFleetStats("C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/vehicleStats.csv");
}
}
