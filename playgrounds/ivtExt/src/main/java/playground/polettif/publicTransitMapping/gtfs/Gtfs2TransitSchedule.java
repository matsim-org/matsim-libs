/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.gtfs;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import playground.polettif.publicTransitMapping.gtfs.lib.GTFSRoute;
import playground.polettif.publicTransitMapping.gtfs.lib.GTFSStop;
import playground.polettif.publicTransitMapping.gtfs.lib.Service;
import playground.polettif.publicTransitMapping.gtfs.lib.Shape;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;
import playground.polettif.publicTransitMapping.tools.ShapeFileTools;

import java.util.*;

/**
 * Contract class to read GTFS files and convert them to an unmapped MATSim Transit Schedule
 *
 * @author polettif
 */
public class Gtfs2TransitSchedule {

	protected TransitSchedule schedule;
	protected Vehicles vehicles;
	protected CoordinateTransformation transformation;

	protected Set<String> serviceIds;
	protected Map<String, GTFSStop> gtfsStops = new HashMap<>();
	protected Map<String, GTFSRoute> gtfsRoutes = new TreeMap<>();
	protected Map<String, Service> services = new HashMap<>();
	protected Map<String, Shape> shapes = new HashMap<>();
	/**
	 * Reads gtfs files in and converts them to an unmapped
	 * MATSim Transit Schedule (mts). "Unmapped" means stopFacilities are not
	 * referenced to links and transit routes do not have routes (link sequences).
	 * Creates a default vehicles file as well.
	 * <p/>
	 *
	 * @param args	[0] folder where the gtfs files are located (a single zip file is not supported)<br/>
	 *              [1] path to the output folder. Will contain the transit schedule, default vehicles file
	 *                  and the converted shape files (if available).
	 * 				[2]	which service ids should be used. One of the following:<br/>
	 *                  <ul>
	 *                  <li>date in the format yyyymmdd</li>
	 *                  <li>dayWithMostTrips</li>
	 *                  <li>dayWithMostServices</li>
	 *                  <li>mostUsedSingleId</li>
	 *                  <li>all</li>
	 *                  </li>
	 *                  </ul>
	 *              [3] the output coordinate system. WGS84/identity transformation is used if not defined.<br/>
	 *
	 * Calls {@link #run}.
	 */
	public static void main(final String[] args) {
		if(args.length == 4) {
			run(args[0], args[1], args[2], args[3]);
		} else if(args.length == 3) {
			run(args[0], args[1], args[2], null);
		} else {
			throw new IllegalArgumentException("Not four input arguments found.");
		}
	}

	/**
	 * Reads gtfs files in and converts them to an unmapped
	 * MATSim Transit Schedule (mts). "Unmapped" means stopFacilities are not
	 * referenced to links and transit routes do not have routes (link sequences).
	 * Creates a default vehicles file as well.
	 * <p/>
	 * @param gtfsFolder          		folder where the gtfs files are located (a single zip file is not supported)
	 * @param outputFolder          	path to the output folder. Will contain the transit schedule, default vehicles file
	 *                              	and the converted shape files (if available).
	 * @param serviceIdsParam        	which service ids should be used. One of the following:
	 *     				             	<ul>
	 *     				             	<li>date in the format yyyymmdd</li>
	 *     				             	<li>dayWithMostTrips</li>
	 *     				             	<li>dayWithMostServices</li>
	 *     				             	<li>mostUsedSingleId</li>
	 *     				             	<li>all</li>
	 *     				             	</ul>
	 * @param outputCoordinateSystem 	the output coordinate system. WGS84/identity transformation is used if <tt>null</tt>.
	 */
	public static void run(String gtfsFolder, String outputFolder, String serviceIdsParam, String outputCoordinateSystem) {
		TransitSchedule schedule = ScheduleTools.createSchedule();
		Vehicles vehicles = ScheduleTools.createVehicles(schedule);
		CoordinateTransformation transformation = outputCoordinateSystem != null ? TransformationFactory.getCoordinateTransformation("WGS84", outputCoordinateSystem) : new IdentityTransformation();

		GtfsConverter gtfsConverter = new GtfsConverter(schedule, vehicles, transformation);
		gtfsConverter.run(gtfsFolder, serviceIdsParam);

		ScheduleTools.writeTransitSchedule(gtfsConverter.getSchedule(), outputFolder + "schedule.xml.gz");
		ScheduleTools.writeVehicles(gtfsConverter.getVehicles(), outputFolder + "vehicles.xml.gz");
		ShapeFileTools.writeGtfsTripsToFile(gtfsConverter.getGtfsRoutes(), gtfsConverter.getServiceIds(), outputCoordinateSystem, outputFolder+"shapes.txt");
	}

	public Gtfs2TransitSchedule(TransitSchedule schedule, Vehicles vehicles, CoordinateTransformation transformation) {
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.transformation = transformation;
	}

	public TransitSchedule getSchedule() {
		return schedule;
	}
	public Vehicles getVehicles() {
		return vehicles;
	}
	public Map<String,GTFSRoute> getGtfsRoutes() {
		return gtfsRoutes;
	}
	public Set<String> getServiceIds() {
		return serviceIds;
	}

}