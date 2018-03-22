/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package org.matsim.contrib.minibus.performance;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleReaderV1;

/**
 * Merges all routes of a transit line that have the same sequence of stops. Does not respect the time profile of the routes.
 * 
 * @author aneumann
 *
 */
public class PTransitLineMerger {
	
	private static final Logger log = Logger.getLogger(PTransitLineMerger.class);
	
	/**
	 * Merges all routes of a transit line that have the same sequence of stops. Does not respect the time profile of the routes.
	 * 
	 * @param transitSchedule to be modified.
	 * @return A copy of the transit schedule with transit lines merged.
	 */
	public static TransitSchedule mergeSimilarRoutes(TransitSchedule transitScheduleOrig){
		printStatistic(transitScheduleOrig);
		
		TransitScheduleFactory transitScheduleFactory = transitScheduleOrig.getFactory();
		TransitSchedule transitSchedule = transitScheduleFactory.createTransitSchedule();
		
		for (TransitStopFacility stop : transitScheduleOrig.getFacilities().values()) {
			transitSchedule.addStopFacility(stop);			
		}
		
		for (TransitLine oldLine : transitScheduleOrig.getTransitLines().values()) {
			TransitLine newLine = mergeTransitLine(transitScheduleFactory, oldLine);
			transitSchedule.addTransitLine(newLine);
		}
		
		printStatistic(transitSchedule);
		return transitSchedule;
	}

	/**
	 * Merges all routes of a transit line that have the same sequence of stops. Does not respect the time profile of the routes.
	 * 
	 * @param oldLine The transit line containing the routes to be merged.
	 * @return A copy of the transit line with its transit routes merged.
	 */
	public static TransitLine mergeTransitLine(TransitLine oldLine) {
		return mergeTransitLine(new TransitScheduleFactoryImpl(), oldLine);
	}
	
	private static TransitLine mergeTransitLine(TransitScheduleFactory transitScheduleFactory, TransitLine oldLine) {
		TransitLine newLine = transitScheduleFactory.createTransitLine(oldLine.getId());
		
		HashMap<String, TransitRoute> routeHash2TransitRoute = new HashMap<String, TransitRoute>();
		
		for (TransitRoute transitRoute : oldLine.getRoutes().values()) {
			String routeHash = getHashForRoute(transitRoute);
			
			if (routeHash2TransitRoute.keySet().contains(routeHash)) {
				// route with same stop sequence exists - merge departures and drop the route
				// Note that this ignores the exact time profile of this route.
				// No severe impact for minibuses since the time profile varies by seconds only
				
				TransitRoute mergeDestination = routeHash2TransitRoute.get(routeHash);
				
				for (Departure oldDeparture : transitRoute.getDepartures().values()) {
					// increase departure id
					Id<Departure> newDepartureId = Id.create("new_" + mergeDestination.getDepartures().values().size(), Departure.class);
					Departure newDeparture = transitScheduleFactory.createDeparture(newDepartureId, oldDeparture.getDepartureTime());
					newDeparture.setVehicleId(oldDeparture.getVehicleId());
					mergeDestination.addDeparture(newDeparture);
				}
				
				// drop the route by not adding it to the new transit line
				
			} else {
				// new route - add it
				routeHash2TransitRoute.put(routeHash, transitRoute);
				newLine.addRoute(transitRoute);
			}
		}
		
		return newLine;
	}
	
	/**
	 * Create a simple hash unique for a certain sequence of stops.
	 * 
	 * @param transitRoute
	 * @return
	 */
	private static String getHashForRoute(TransitRoute transitRoute){
		StringBuffer strB = null;
		for (TransitRouteStop transitStop : transitRoute.getStops()) {
			if (strB == null) {
				strB = new StringBuffer();
			} else {
				strB.append("-");
			}
			strB.append(transitStop.getStopFacility().getId().toString());
		}
		
		return strB.toString();
	}
	
	private static void printStatistic(TransitSchedule transitSchedule){
		int nStops = 0;
		int nLines = 0;
		int nRoutes = 0;
		int nDepartures = 0;
		
		nStops = transitSchedule.getFacilities().values().size();
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			nLines++;
			
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				nRoutes++;
				nDepartures += transitRoute.getDepartures().size();
			}
		}
		
		log.info("Transit schedule stats: " + nStops + " stops, " + nLines + " lines, " + nRoutes + " routes, " + nDepartures + " departures.");
	}

	public static void main(String[] args) {
		String folder = "e:/transitScheduleTest/";
		String networkFile = folder + "network.final.xml.gz";
		String transitScheduleInFile = folder + "bvg6_b_0.1250.transitSchedule.xml.gz";
		String transitScheduleOutFile = folder + "bvg6_b_0.1250.transitSchedule_merged.xml.gz";
		String vehicleFile = folder + "bvg6_b_0.1250.vehicles.xml.gz";
		
		Scenario scenario =  ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFile);
		
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(scenario.getTransitVehicles());
		vehicleReader.readFile(vehicleFile);
		TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
		scheduleReader.readFile(transitScheduleInFile);
		
		PTransitLineMerger.mergeSimilarRoutes(scenario.getTransitSchedule());
		
		TransitScheduleWriter writer = new TransitScheduleWriter(scenario.getTransitSchedule());
		writer.writeFile(transitScheduleOutFile);
	}
}
