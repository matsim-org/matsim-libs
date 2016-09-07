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

package playground.polettif.publicTransitMapping.hafas;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import playground.polettif.publicTransitMapping.hafas.lib.BitfeldAnalyzer;
import playground.polettif.publicTransitMapping.hafas.lib.OperatorReader;
import playground.polettif.publicTransitMapping.hafas.lib.StopReader;
import playground.polettif.publicTransitMapping.hafas.v1.FPLANReaderV1;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Hafas2MATSimTransitSchedule.
 *
 * @author boescpa
 */
public class HafasConverterV1OperatorAsLine extends Hafas2TransitSchedule {

	public HafasConverterV1OperatorAsLine(TransitSchedule schedule, Vehicles vehicles, CoordinateTransformation transformation) {
		super(schedule, vehicles, transformation);
	}

	@Override
	public void createSchedule(String pathToInputFiles) throws IOException {
		log.info("Creating the schedule based on HAFAS...");

		// 1. Read and create stop facilities
		log.info("  Read transit stops...");
		StopReader.run(schedule, transformation, pathToInputFiles + "BFKOORD_GEO");
		log.info("  Read transit stops... done.");

		// 2. Read all operators from BETRIEB_DE
		log.info("  Read operators...");
		Map<String, String> operators = OperatorReader.readOperators(pathToInputFiles + "BETRIEB_DE");
		log.info("  Read operators... done.");

		// 3. Read all ids for work-day-routes from HAFAS-BITFELD
		log.info("  Read bitfeld numbers...");
		Set<Integer> bitfeldNummern = BitfeldAnalyzer.findBitfeldnumbersOfBusiestDay(pathToInputFiles + "FPLAN", pathToInputFiles + "BITFELD");
		log.info("  Read bitfeld numbers... done.");

		// 4. Create all lines from HAFAS-Schedule
		log.info("  Read transit lines...");
		FPLANReaderV1.readLines(schedule, vehicles, bitfeldNummern, operators, pathToInputFiles + "FPLAN");
		log.info("  Read transit lines... done.");

		// 5. Clean schedule
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
		ScheduleCleaner.uniteSameRoutesWithJustDifferentDepartures(schedule);
		ScheduleCleaner.cleanDepartures(schedule);
		ScheduleCleaner.cleanVehicles(schedule, vehicles);

		log.info("Creating the schedule based on HAFAS... done.");
	}

}
