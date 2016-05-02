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

package playground.polettif.boescpa.lib.tools.spatialCutting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import playground.polettif.boescpa.lib.tools.coordUtils.CoordFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * Geographically cuts a MATSim schedule to a specified area.
 *
 * @author boescpa
 */
public class ScheduleCutter {

	private final static Logger log = Logger.getLogger(ScheduleCutter.class);
	private final CoordFilter coordFilter;
	private final TransitSchedule schedule;
	private final Vehicles vehicles;

	public ScheduleCutter(TransitSchedule schedule, Vehicles vehicles, CoordFilter coordFilter) {
		this.coordFilter = coordFilter;
		this.schedule = schedule;
		this.vehicles = vehicles;
	}

	/**
	 * args 0: schedule file
	 * args 1: X-coord center (double)
	 * args 2: Y-coord center (double)
	 * args 3: Radius (int)
	 * args 4: Path to schedule-output
	 */
	public static void main(String[] args) {
		// For 30km around Zurich Center (Bellevue): X - 2683518.0, Y - 1246836.0, radius - 30000

		// Read schedule
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getConfig().transit().setUseTransit(true);

		new TransitScheduleReader(scenario).readFile(args[0]);
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
//		new VehicleReaderV1(vehicles).readFile(config.transit().getVehiclesFile());
		vehicles = null;

		// Cut schedule
		Coord center = new Coord(Double.parseDouble(args[1]), Double.parseDouble(args[2]));
		int radius = Integer.parseInt(args[3]);
		log.info(" Area of interest (AOI): center=" + center + "; radius=" + radius);
		ScheduleCutter cutter = new ScheduleCutter(
				scenario.getTransitSchedule(), vehicles,
				new CoordFilter.CoordFilterCircle(center, radius));
		cutter.cutSchedule();

		// Write schedule and vehicles
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(args[4]);
//		new VehicleWriterV1(vehicles).writeFile(args[5]);

		// Test schedule and vehicles
//		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new TransitScheduleReader(scenario).readFile(args[4]);
//		vehicles = VehicleUtils.createVehiclesContainer();
//		new VehicleReaderV1(vehicles).readFile(config.transit().getVehiclesFile());
	}

	public void cutSchedule() {
		// Identify all stops inside specified area:
		Set<Id<TransitStopFacility>> stopsInArea = new HashSet<>();
		for (TransitStopFacility stop : schedule.getFacilities().values()) {
			if (coordFilter.coordCheck(stop.getCoord())) {
				stopsInArea.add(stop.getId());
			}
		}
		log.info(" AOI contains: " + stopsInArea.size() + " stops.");

		// Identify all routes not crossing area and therefore to remove:
		int routesRemoved = 0;
		int vehiclesRemoved = 0;
		Set<TransitLine> linesToRemove = new HashSet<>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			Set<TransitRoute> routesToRemove = new HashSet<>();
			for (TransitRoute route : line.getRoutes().values()) {
				boolean toKeep = false;
				for (TransitRouteStop stop : route.getStops()) {
					if (stopsInArea.contains(stop.getStopFacility().getId())) {
						toKeep = true;
					}
				}
				if (!toKeep) {
					routesToRemove.add(route);
				}
			}
			// Remove identified routes (and their vehicles):
			for (TransitRoute routeToRemove : routesToRemove) {
				line.removeRoute(routeToRemove);
				if (vehicles != null) {
					vehicles.removeVehicle(routeToRemove.getRoute().getVehicleId());
					vehiclesRemoved++;
				}
				routesRemoved++;
			}
			if (line.getRoutes().isEmpty()) {
				linesToRemove.add(line);
			}
		}
		log.info(" Routes removed: " + routesRemoved);
		log.info(" Vehicles removed: " + vehiclesRemoved);
		// Remove empty lines:
		for (TransitLine lineToRemove : linesToRemove) {
			schedule.removeTransitLine(lineToRemove);
		}
		log.info(" Lines removed: " + linesToRemove.size());

		// Identify and remove unused stops:
		Set<Id<TransitStopFacility>> stopsToKeep = new HashSet<>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					stopsToKeep.add(stop.getStopFacility().getId());
				}
			}
		}
		Set<TransitStopFacility> stopsToRemove = new HashSet<>();
		for (TransitStopFacility stop : schedule.getFacilities().values()) {
			if (!stopsToKeep.contains(stop.getId())) {
				stopsToRemove.add(stop);
			}
		}
		for (TransitStopFacility stopToRemove : stopsToRemove) {
			schedule.removeStopFacility(stopToRemove);
		}
		log.info(" Stops removed: " + stopsToRemove.size());
	}

}
