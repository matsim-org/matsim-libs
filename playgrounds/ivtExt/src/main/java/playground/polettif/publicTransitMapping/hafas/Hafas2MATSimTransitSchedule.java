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

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import playground.polettif.publicTransitMapping.hafas.lib.*;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author polettif
 */
public class Hafas2MATSimTransitSchedule {

	protected static Logger log = Logger.getLogger(Hafas2MATSimTransitSchedule.class);

	protected final TransitSchedule schedule;
	protected final Vehicles vehicles;
	protected final CoordinateTransformation transformation;

	/**
	 * Converts all files in <tt>hafasFolder</tt> and writes the output schedule and vehicle file to
	 * <tt>outputFolder</tt>. Stop Facility coordinates are transformed to <tt>outputCoordinateSystem</tt>.
	 *
	 * @param args <br/>
	 *             [0] hafasFolder<br/>
	 *             [1] outputFolder<br/>
	 *             [2] outputCoordinateSystem<br/>
	 */
	public static void main(String[] args) {
		run(args[0], args[1], args[2]);
	}

	/**
	 * Converts all files in <tt>hafasFolder</tt> and writes the output schedule and vehicle file to
	 * <tt>outputFolder</tt>. Stop Facility coordinates are transformed to <tt>outputCoordinateSystem</tt>.
	 */
	public static void run(String hafasFolder, String outputFolder, String outputCoordinateSystem) {
		TransitSchedule schedule = ScheduleTools.createSchedule();
		Vehicles vehicles = ScheduleTools.createVehicles(schedule);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", outputCoordinateSystem);

		new Hafas2MATSimTransitSchedule(schedule, vehicles, transformation).createSchedule(hafasFolder);

		ScheduleTools.writeTransitSchedule(schedule, outputFolder+"schedule.xml");
		ScheduleTools.writeVehicles(vehicles, outputFolder+"vehicles.xml");
	}

	public Hafas2MATSimTransitSchedule(TransitSchedule schedule, Vehicles vehicles, CoordinateTransformation transformation) {
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.transformation = transformation;
	}

	/**
	 * This method creates a Transit Schedule which is unlinked to any network, but else complete.
	 */
	public void createSchedule(String pathToInputFiles) {
		log.info("Creating the schedule based on HAFAS...");

		// 1. Read and create stop facilities
		log.info("  Read transit stops...");
		StopReader.run(schedule, transformation, pathToInputFiles + "BFKOORD_GEO");
		log.info("  Read transit stops... done.");

		// 1. Read all operators from BETRIEB_DE
		log.info("  Read operators...");
		Map<String, String> operators = OperatorReader.readOperators(pathToInputFiles + "BETRIEB_DE");
		log.info("  Read operators... done.");

		// 2. Read all ids for work-day-routes from HAFAS-BITFELD
		log.info("  Read bitfeld numbers...");
		Set<Integer> bitfeldNummern = BitfeldAnalyzer.findBitfeldnumbersOfBusiestDay(pathToInputFiles + "FPLAN", pathToInputFiles + "BITFELD");
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
