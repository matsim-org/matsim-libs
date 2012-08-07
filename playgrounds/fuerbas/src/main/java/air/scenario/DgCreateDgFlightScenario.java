/* *********************************************************************** *
 * project: org.matsim.*
 * DgCreateScenario
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.scenario;

import java.io.File;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import air.analysis.DgNet2Shape;


/**
 * @author dgrether
 *
 */
public class DgCreateDgFlightScenario {

	private static String utcOffsetfile = "/media/data/work/repos/shared-svn/studies/countries/world/flight/sf_oag_flight_model/utc_offsets.txt";
	private static String dataBaseDirectory = "/media/data/work/repos/";
	private static String inputAirportsFilename = dataBaseDirectory + "shared-svn/studies/countries/world/flight/sf_oag_flight_model/worldwide_airports_with_coords.csv";
	private static String inputOagFilename = dataBaseDirectory + "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";
	private static final String OAG_FLIGHTS_OUTPUT_FILENAME = "oag_flights.txt";

	private static final String FLIGHT_TRANSIT_SCHEDULE = "flight_transit_schedule.xml";
	
	private static final String FLIGHT_TRANSIT_VEHICLES = "flight_transit_vehicles.xml";

	

	public static void createGermanFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception{
		Config conf = ConfigUtils.createConfig();
		conf.scenario().setUseTransit(true);
		conf.scenario().setUseVehicles(true);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(conf);
		
		String baseDirectory = "/media/data/work/repos/shared-svn/studies/countries/de/flight/dg_oag_flight_model/";
		OutputDirectoryLogging.initLoggingWithOutputDirectory(baseDirectory);
		String oagFlightsFilename = baseDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;
		
		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		DgOagFlightsData flightsData = airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory,
				SfAirScheduleBuilder.GERMAN_COUNTRIES, utcOffsetfile, oagFlightsFilename);
		Map<String, Coord> airports = airScheduleBuilder.getAirportCoordMap();

		String outputNetworkFilename = baseDirectory + "air_network.xml";
//		String outputOsmAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_OUTPUT_FILE;
//		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
//
		DgAirNetworkBuilder networkBuilder = new DgAirNetworkBuilder(scenario);
		networkBuilder.createNetwork(flightsData, airports, outputNetworkFilename);
		Map<Id, SfMatsimAirport> airportMap = networkBuilder.getAirportMap();
		//
		DgTransitBuilder transitBuilder = new DgTransitBuilder(scenario);
		transitBuilder.createSchedule(flightsData, airportMap);
		transitBuilder.writeTransitFile(baseDirectory + FLIGHT_TRANSIT_SCHEDULE, baseDirectory + FLIGHT_TRANSIT_VEHICLES);
//
//		writeShape(baseDirectory, inputNetworkFile);
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	public static void writeShape(String baseDirectory, String networkFilename){

		//	String shapeFileDirectoryname = baseDirectory + "shape_epsg_3395\\"; //for windows file systems
		String shapeFileDirectoryname = baseDirectory + "shape_epsg_3395/";
		File shapeFileDirectory = new File(shapeFileDirectoryname);
		if (shapeFileDirectory.exists()){
			shapeFileDirectory.delete();
		}
		shapeFileDirectory.mkdir();
		DgNet2Shape.writeNetwork2Shape(networkFilename, shapeFileDirectoryname + SfAirNetworkBuilder.NETWORK_FILENAME + ".shp");
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//WORLD WIDE AIR TRAFFIC
		//	createWorldFlightScenario(inputAirportsFilename, inputOagFilename);

		//EUROPEAN AIR TRAFFIC
		//	createEuropeanFlightScenario(inputAirportsFilename, inputOagFilename);

		// GERMAN AIR TRAFFIC
		createGermanFlightScenario(inputAirportsFilename, inputOagFilename);
	}

}
