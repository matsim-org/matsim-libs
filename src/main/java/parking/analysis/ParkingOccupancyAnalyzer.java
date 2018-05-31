/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package parking.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import parking.ZonalLinkParkingInfo;
import parking.capacityCalculation.UseParkingCapacityFromNetwork;

public class ParkingOccupancyAnalyzer {
public static void main(String[] args) {
	String basefolder = "D:/runs-svn/vw_rufbus/";
	String runId = "vw220park10T";
	String eventsFile = basefolder+runId+"/"+runId+".output_events.xml.gz";
	String populationFile = basefolder+runId+"/"+runId+".output_plans.xml.gz";
	String parkingOccupancyOutputFile = basefolder+runId+"/"+runId+".output_parkingOccupancy.csv";
	String relparkingOccupancyOutputFile = basefolder + runId + "/" + runId + ".output_parkingOccupancy_relative.csv";
	String parkingTripsOutputFile = basefolder+runId+"/"+runId+".output_parkingTrips.csv";
	String networkFile = basefolder+runId+"/"+runId+".output_network.xml.gz";
	String shapeFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/parking/bc-run/shp/parking-zones.shp";
	String shapeString = "NO";
	double endTime = 30*3600;
	
	Network network = NetworkUtils.createNetwork();
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile(populationFile);
	new MatsimNetworkReader(network).readFile(networkFile);
//	LinkLengthBasedCapacityCalculator  linkLengthBasedCapacityCalculator = new LinkLengthBasedCapacityCalculator();
	UseParkingCapacityFromNetwork useParkingCapacityFromNetwork = new UseParkingCapacityFromNetwork();
	ZonalLinkParkingInfo zonalLinkParkingInfo = new ZonalLinkParkingInfo(shapeFile, shapeString, 0.1, network, useParkingCapacityFromNetwork, scenario.getPopulation());
	ParkingOccupancyEventHandler parkingOccupancyEventHandler = new ParkingOccupancyEventHandler(zonalLinkParkingInfo, useParkingCapacityFromNetwork, network, endTime, 0.1);
	parkingOccupancyEventHandler.reset(0);
	ParkingTripHandler tripHandler = new ParkingTripHandler(network, zonalLinkParkingInfo,scenario.getPopulation());
	EventsManager events = EventsUtils.createEventsManager();
	events.addHandler(parkingOccupancyEventHandler);
	events.addHandler(tripHandler);
	new MatsimEventsReader(events).readFile(eventsFile);
	parkingOccupancyEventHandler.writeParkingOccupancyStats(parkingOccupancyOutputFile, relparkingOccupancyOutputFile);
	tripHandler.writeParkingTrips(parkingTripsOutputFile);
}
}
