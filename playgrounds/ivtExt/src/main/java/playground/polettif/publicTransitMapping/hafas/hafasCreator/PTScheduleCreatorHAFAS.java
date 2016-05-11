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

package playground.polettif.publicTransitMapping.hafas.hafasCreator;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import playground.polettif.publicTransitMapping.hafas.DeparturesCreator;
import playground.polettif.publicTransitMapping.hafas.PTScheduleCreator;
import playground.polettif.publicTransitMapping.hafas.RouteProfileCreator;
import playground.polettif.publicTransitMapping.hafas.TransitStopCreator;

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

		// 1. Read all operators from BETRIEB_DE
		log.info("  Read operators...");
		Map<String, String> operators = OperatorReader.readOperators(pathToInputFiles + "BETRIEB_DE");
		log.info("  Read operators... done.");

		// 2. Read all ids for work-day-routes from HAFAS-BITFELD
		log.info("  Read bitfeld numbers...");
		Set<Integer> bitfeldNummern =
				BitfeldAnalyzer.findBitfeldnumbersOfBusiestDay(pathToInputFiles + "FPLAN", pathToInputFiles + "BITFELD");
		log.info("  Read bitfeld numbers... done.");

		// 3. Create all lines from HAFAS-Schedule
		log.info("  Read transit lines...");
		FPLANReader.readLines(schedule, vehicles, bitfeldNummern, operators, pathToInputFiles + "FPLAN");
		log.info("  Read transit lines... done.");

		// 4. Clean schedule
		HAFASUtils.removeNonUsedStopFacilities(schedule);
		HAFASUtils.uniteSameRoutesWithJustDifferentDepartures(schedule);
		HAFASUtils.cleanDepartures(schedule);
		HAFASUtils.cleanVehicles(schedule, vehicles);

		log.info("Creating the schedule based on HAFAS... done.");
	}
}
