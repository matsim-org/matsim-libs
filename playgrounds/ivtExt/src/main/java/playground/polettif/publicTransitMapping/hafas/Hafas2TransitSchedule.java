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
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.io.IOException;

/**
 * Contract class to convert (Swiss) HAFAS data to
 * an unmapped MATSim Transit Schedule.
 *
 * @author polettif
 */
public abstract class Hafas2TransitSchedule {

	protected static Logger log = Logger.getLogger(Hafas2TransitSchedule.class);

	protected final TransitSchedule schedule;
	protected final Vehicles vehicles;
	protected final CoordinateTransformation transformation;

	/**
	 * Converts all files in <tt>hafasFolder</tt> and writes the output schedule and vehicle file to
	 * <tt>outputFolder</tt>. Stop Facility coordinates are transformed to <tt>outputCoordinateSystem</tt>.
	 *
	 * @param args <br/>
	 *             [0] hafasFolder<br/>
	 *             [1] outputCoordinateSystem<br/>
	 *             [2] outputScheduleFile<br/>
	 *             [3] outputVehicleFile<br/>
	 */
	public static void main(String[] args) throws IOException {
		run(args[0], args[1], args[2], args[3]);
	}

	/**
	 * Converts all files in <tt>hafasFolder</tt> and writes the output schedule and vehicle file to
	 * <tt>outputFolder</tt>. Stop Facility coordinates are transformed to <tt>outputCoordinateSystem</tt>.
	 */
	public static void run(String hafasFolder, String outputCoordinateSystem, String outputScheduleFile, String outputVehicleFile) throws IOException {
		TransitSchedule schedule = ScheduleTools.createSchedule();
		Vehicles vehicles = ScheduleTools.createVehicles(schedule);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", outputCoordinateSystem);

		new HafasConverter(schedule, vehicles, transformation).createSchedule(hafasFolder);

		ScheduleTools.writeTransitSchedule(schedule, outputScheduleFile);
		ScheduleTools.writeVehicles(vehicles, outputVehicleFile);
	}

	/**
	 * Constructor
	 */
	public Hafas2TransitSchedule(TransitSchedule schedule, Vehicles vehicles, CoordinateTransformation transformation) {
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.transformation = transformation;
	}

	/**
	 * This method creates a Transit Schedule which is unlinked to any network, but else complete.
	 */
	public abstract void createSchedule(String pathToInputFiles) throws IOException;
}
