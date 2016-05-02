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

package playground.polettif.multiModalMap.validation;

import com.opencsv.CSVReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.gtfs.GTFSReader;
import playground.polettif.multiModalMap.gtfs.containers.GTFSDefinitions;
import playground.polettif.multiModalMap.gtfs.containers.Shape;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RouteShapeValidator {

	private final TransitSchedule schedule;
	private final TransitScheduleFactory scheduleFactory;
	private final Network network;
	private Map<String, Shape> gtfsShapes;
	private Map<String, Shape> scheduleShapes;

	public RouteShapeValidator(TransitSchedule schedule, Network network) {
		this.schedule = schedule;
		this.scheduleFactory = schedule.getFactory();
		this.network = network;
	}

	public static void main(final String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new TransitScheduleReader(scenario).readFile(args[0]);
		new MatsimNetworkReader(network).readFile(args[1]);
		TransitSchedule schedule = scenario.getTransitSchedule();

		RouteShapeValidator validator = new RouteShapeValidator(schedule, network);
		validator.readShapes(args[2]);

	}

	public void readShapes(String filePath) throws IOException {
		gtfsShapes = new HashMap<>();
		CSVReader reader = new CSVReader(new FileReader(filePath), ';');

		String[] header = reader.readNext();
		Map<String, Integer> col = GTFSReader.getIndices(header, GTFSDefinitions.SHAPES.columns);

		String[] line = reader.readNext();
		while(line != null) {
			Shape actual = gtfsShapes.get(line[col.get("shape_id")]);
			if(actual == null) {
				actual = new Shape(line[col.get("shape_id")]);
				gtfsShapes.put(line[col.get("shape_id")], actual);
			}
			actual.addPoint(new Coord(Double.parseDouble(line[col.get("shape_pt_lat")]), Double.parseDouble(line[col.get("shape_pt_lon")])), Integer.parseInt(line[col.get("shape_pt_sequence")]));

			line = reader.readNext();
		}
		reader.close();
	}

	public void convertScheduleToShapes() {
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {

			}
		}
	}

}