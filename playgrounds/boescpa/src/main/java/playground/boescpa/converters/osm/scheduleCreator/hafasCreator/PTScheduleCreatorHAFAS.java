/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.osm.scheduleCreator.hafasCreator;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import playground.boescpa.converters.osm.scheduleCreator.DeparturesCreator;
import playground.boescpa.converters.osm.scheduleCreator.PTScheduleCreator;
import playground.boescpa.converters.osm.scheduleCreator.RouteProfileCreator;
import playground.boescpa.converters.osm.scheduleCreator.TransitStopCreator;

import java.util.Map;
import java.util.Set;

/**
 * The HAFAS implementation of PTScheduleCreator (using the Swiss-HAFAS-Schedule).
 *
 * @author boescpa
 */
public class PTScheduleCreatorHAFAS extends PTScheduleCreator implements RouteProfileCreator, DeparturesCreator {

	private CoordinateTransformation transformation;

	public PTScheduleCreatorHAFAS(TransitSchedule schedule, Vehicles vehicles, CoordinateTransformation transformation) {
		super(schedule, vehicles);
		this.transformation = transformation;
	}

	@Override
	public TransitStopCreator getTransitStopCreator() {
		return new StopReader(transformation);
	}

	@Override
	public RouteProfileCreator getRouteProfileCreator() {
		return this;
	}

	@Override
	public DeparturesCreator getDeparturesCreator() {
		return this;
	}

	@Override
	public void createRouteProfiles(TransitSchedule schedule, String pathToInputFiles) {
		// Does nothing as we do in this implementation all in the createDepartures step.
	}

	@Override
	public void createDepartures(TransitSchedule schedule, Vehicles vehicles, String pathToInputFiles) {
		log.info("Creating the schedule based on HAFAS...");

		// 1. Read all vehicles from vehicleFile:
		log.info("  Read vehicles...");
		VehicleTypesReader.readVehicles(vehicles, pathToInputFiles + "VehicleData.csv");
		log.info("  Read vehicles... done.");

		// 2. Read all operators from BETRIEB_DE
		log.info("  Read operators...");
		Map<String, String> operators = OperatorReader.readOperators(pathToInputFiles + "/HAFAS/BETRIEB_DE");
		log.info("  Read operators... done.");

		// 3. Read all ids for work-day-routes from HAFAS-BITFELD
		log.info("  Read bitfeld numbers...");
		Set<Integer> bitfeldNummern =
				BitfeldAnalyzer.findBitfeldnumbersOfBusiestDay(pathToInputFiles + "HAFAS/FPLAN", pathToInputFiles + "HAFAS/BITFELD");
		log.info("  Read bitfeld numbers... done.");

		// 4. Create all lines from HAFAS-Schedule
		log.info("  Read transit lines...");
		Map<String, Integer> vehiclesUndefined =
				FPLANReader.readLines(schedule, vehicles, bitfeldNummern, operators, pathToInputFiles + "HAFAS/FPLAN");
		log.info("  Read transit lines... done.");

		// 5. Print undefined vehicles
		for (String vehicleUndefined : vehiclesUndefined.keySet()) {
			log.warn("Undefined vehicle " + vehicleUndefined + " occured in " + vehiclesUndefined.get(vehicleUndefined) + " routes.");
		}

		// 6. Clean schedule
		HAFASUtils.removeNonUsedStopFacilities(schedule);
		HAFASUtils.uniteSameRoutesWithJustDifferentDepartures(schedule);
		HAFASUtils.cleanDepartures(schedule);
		HAFASUtils.cleanVehicles(schedule, vehicles);

		log.info("Creating the schedule based on HAFAS... done.");
	}
}
