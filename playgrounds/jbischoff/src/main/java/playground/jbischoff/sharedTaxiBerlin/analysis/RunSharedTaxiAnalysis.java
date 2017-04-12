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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import playground.jbischoff.utils.JbUtils;

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
	String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/output_network.xml.gz";
	Network network = NetworkUtils.createNetwork();
	new MatsimNetworkReader(network).readFile(networkFile);
	EventsManager events = EventsUtils.createEventsManager();
	DynModePassengerStats drtStats = new DynModePassengerStats(network,"drt");
	DrtVehicleOccupancyEvaluator vehicleOccupancyEvaluator = new DrtVehicleOccupancyEvaluator(16*3600, 32*3600, 4);
	events.addHandler(vehicleOccupancyEvaluator);
	events.addHandler(drtStats);
	new MatsimEventsReader(events).readFile(eventsFile);
	vehicleOccupancyEvaluator.writeDetailedOccupancyFiles("C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/vehicles/");
	vehicleOccupancyEvaluator.calcAndWriteFleetStats("C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/vehicleStats.csv");
	List<DynModeTrip> trips = drtStats.getDrtTrips();
	JbUtils.collection2Text(trips, "C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/drtrips.csv", DynModeTrip.HEADER);
	DynModeTripsAnalyser.analyseWaitTimes("C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/waitstats", trips, 1800);
	DynModeTripsAnalyser.analyseDetours(network, trips, 1.3, 4.16, "C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/runs/10_pct_prerun_100veh/detours");
//	Collections.sort(trips);	
//	for (DrtTrip trip : trips){
//		System.out.println(trip.toString());
//	}
//	Map<Double,List<DrtTrip>> splitTrips = DrtTripsAnalyser.splitTripsIntoBins(trips, 16*3600, 32*3600, 3600);
	
//	for (Entry<Double, List<DrtTrip>> e : splitTrips.entrySet()){
//		System.out.println("time " +e.getKey()/3600);
//		for (DrtTrip trip : e.getValue()){
//			System.out.println(trip.toString());
//		}
//	}
}
}
