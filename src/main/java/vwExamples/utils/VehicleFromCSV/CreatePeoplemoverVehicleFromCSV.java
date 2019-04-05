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
package vwExamples.utils.VehicleFromCSV;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
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

public class CreatePeoplemoverVehicleFromCSV {
	ArrayList<Coord> Locations;
	String csvInitalVehiclePosition;
	String networkFile;
	Network network;
	List<String[]> CSVData;
	String vehicleOutputFile;
	Network vehiclenetwork;
	List<DvrpVehicleSpecification> vehicles;
	int seats;
	int operationStartTimet;
	int operationsEndTime;

	public CreatePeoplemoverVehicleFromCSV(String csvInitalVehiclePosition, String networkFile, String vehicleOutputFile) {
		this.csvInitalVehiclePosition = csvInitalVehiclePosition;
		this.networkFile = networkFile;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		this.network = scenario.getNetwork();
		this.Locations = new ArrayList<Coord>();
		this.vehicleOutputFile = vehicleOutputFile;
		this.vehicles = new ArrayList<>();
		this.seats = 6;
		this.operationStartTimet = 0;
		this.operationsEndTime = 30000;

	}

	public static void main(String[] args) {

		CreatePeoplemoverVehicleFromCSV stopData = new CreatePeoplemoverVehicleFromCSV(
				"D:\\Matsim\\Axer\\Hannover\\MOIA\\input\\simulation_input_data_to_steffen\\preinitialized_hannover_vehicle_locations.csv",
				"D:\\Matsim\\Axer\\Hannover\\MOIA\\input\\network\\network.xml.gz",
				"D:\\Matsim\\Axer\\Hannover\\MOIA\\input\\fleets\\vehicles_MOIA.xml.gz");
		stopData.readVehicleLocationsCSV();
		stopData.createFleet(500);
	}

	public void createFleet(int fleetsize) {
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

		this.vehiclenetwork = nfm.applyFilters();

		for (int i = 1; i < fleetsize; i++) {
			int vehicleIdx = i%Locations.size();
			//System.out.println(vehicleIdx);
			Coord vehicleLocation = Locations.get(vehicleIdx);

			Link vehicleLink = NetworkUtils.getNearestLink(vehiclenetwork, vehicleLocation);

			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create("drt_" + i, DvrpVehicle.class))
					.startLinkId(vehicleLink.getId())
					.capacity(seats)
					.serviceBeginTime(this.operationStartTimet)
					.serviceEndTime(this.operationsEndTime)
					.build();
		    vehicles.add(v);  

		}
		new FleetWriter(vehicles.stream()).write(this.vehicleOutputFile);

	}

	public void readVehicleLocationsCSV() {
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				"EPSG:25832");

		// CSV HEADER
		// lon,lat,bearing

		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(this.csvInitalVehiclePosition));
			CSVData = reader.readAll();
			for (int i = 1; i < CSVData.size(); i++) {
				String[] lineContents = CSVData.get(i);
				double lat = Double.parseDouble(lineContents[0]); // lat,
				double lon = Double.parseDouble(lineContents[1]); // lon,

				Coord location = ct.transform(new Coord(lon,lat ));
				Locations.add(location);
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
