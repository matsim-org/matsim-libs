/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package vwExamples.utils.StopFromCSV;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.opencsv.CSVReader;

import vwExamples.utils.DemandFromCSV.Trip;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreatePeoplemoverStopsFromCSV {
	ArrayList<Stop> Stops;
	String csvStopFile;
	String networkFile;
	Network network;
	List<String[]> CSVData;
	String stopOutputFile;
	Network stopnetwork;

	CreatePeoplemoverStopsFromCSV(String csvStopFile, String networkFile, String stopOutputFile) {
		this.csvStopFile = csvStopFile;
		this.networkFile = networkFile;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		this.network = scenario.getNetwork();
		this.Stops = new ArrayList<Stop>();
		this.stopOutputFile = stopOutputFile;

	}

	public static void main(String[] args) {

		CreatePeoplemoverStopsFromCSV stopData = new CreatePeoplemoverStopsFromCSV(
				"D:\\Matsim\\Axer\\Hannover\\MOIA\\input\\simulation_input_data_to_steffen\\active_simulation_stops_hannover_2018-10-18.csv",
				"D:\\Matsim\\Axer\\Hannover\\MOIA\\input\\network\\network.xml.gz",
				"D:\\Matsim\\Axer\\Hannover\\MOIA\\input\\network\\virtualStops_MOIA.xml");
		stopData.readStopsCSV();
		stopData.createStopFacilities();
	}

	public void createStopFacilities() {
		TransitScheduleFactory f = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = f.createTransitSchedule();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				"EPSG:25832");

		NetworkFilterManager nfm = new NetworkFilterManager(this.network);
		nfm.addLinkFilter(new NetworkLinkFilter() {

			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains("car")) {
					return true;
				} else
					return false;
			}
		});

		this.stopnetwork = nfm.applyFilters();

		for (Stop stop : Stops) {
			Coord stopCoord = ct.transform(new Coord(stop.lat, stop.lon));

			// Search next link to stopCoord

			Link forwardLink = NetworkUtils.getNearestLink(this.stopnetwork, stopCoord);
			Link backwordLink = NetworkUtils.findLinkInOppositeDirection(forwardLink);


			Id<TransitStopFacility> forwardKey = Id.create(forwardLink.getId() + "_stop", TransitStopFacility.class);

			if (!schedule.getFacilities().containsKey(forwardKey)) {
				TransitStopFacility forwardStop = f.createTransitStopFacility(forwardKey, forwardLink.getCoord(),
						false);
				schedule.addStopFacility(forwardStop);
			}

//			if (backwordLink != null) {
//				Id<TransitStopFacility> backwardKey = Id.create(backwordLink.getId() + "_stop",
//						TransitStopFacility.class);
//				if (!schedule.getFacilities().containsKey(backwardKey)) {
//					TransitStopFacility backwardStop = f.createTransitStopFacility(backwardKey, backwordLink.getCoord(),
//							false);
//					schedule.addStopFacility(backwardStop);
//				}
//
//			}

		}
		new TransitScheduleWriter(schedule).writeFile(this.stopOutputFile);

	}

	public void readStopsCSV() {

		// CSV HEADER
		// lon,lat,bearing

		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(this.csvStopFile));
			CSVData = reader.readAll();
			for (int i = 1; i < CSVData.size(); i++) {
				String[] lineContents = CSVData.get(i);
				double lat = Double.parseDouble(lineContents[0]); // lat,
				double lon = Double.parseDouble(lineContents[1]); // lon,
				double bearing = Double.parseDouble(lineContents[2]); // bearing,

				Stop stop = new Stop(lat, lon, bearing);
				Stops.add(stop);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
