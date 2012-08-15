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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import air.analysis.DgNet2Shape;


/**
 * @author dgrether
 *
 */
public class DgCreateDgFlightScenario {

	private static final Logger log = Logger.getLogger(DgCreateDgFlightScenario.class);
	
	private static final double CAPACITY_PERIOD = 3600.0;
	
	private static final String OAG_FLIGHTS_OUTPUT_FILENAME = "oag_flights.txt";
	private static final String FLIGHT_TRANSIT_SCHEDULE = "flight_transit_schedule.xml";
	private static final String FLIGHT_TRANSIT_VEHICLES = "flight_transit_vehicles.xml";

	private static String dataBaseDirectory = "/media/data/work/repos/";
	private static String utcOffsetfile = dataBaseDirectory + "shared-svn/studies/countries/world/flight/sf_oag_flight_model/utc_offsets.txt";
	public static String inputAirportsCoordinatesFilename = dataBaseDirectory + "shared-svn/studies/countries/world/flight/sf_oag_flight_model/worldwide_airports_with_coords.csv";
	public static String inputOagFilename = dataBaseDirectory + "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";
	private String flightScenarioDirectoryName = "dg_oag_flight_model_2_runways_airport_capacities_www/";
	private CoordinateReferenceSystem targetCrs = MGC.getCRS("EPSG:3395");
	private CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395");
	private DgFlightScenarioData scenarioData = new DgFlightScenarioData(CAPACITY_PERIOD);
	private boolean useAirportCapacities = true;
	private String airportCapacityFile = dataBaseDirectory + "shared-svn/projects/throughFlightData/airportCapacityData/2012-08-14_airport_capacity_from_www.csv";
	
	private ScenarioImpl initScenario(){
		Config conf = ConfigUtils.createConfig();
		conf.scenario().setUseTransit(true);
		conf.scenario().setUseVehicles(true);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(conf);
		((NetworkImpl)scenario.getNetwork()).setCapacityPeriod(CAPACITY_PERIOD);
		return scenario;
	}

	private void readAirportCapacities(DgFlightScenarioData scenarioData){
		new DgAirportCapacityReader(scenarioData.getAirportsCapacityData()).readFile(airportCapacityFile);
	}
	

	private void createScenario(String baseDirectory, DgOagFlightsData flightsData, Map<String, Coord> airports){
		log.info("Coordinate system is: "); 
		log.info(targetCrs.getCoordinateSystem().getRemarks());
		String outputNetworkFilename = baseDirectory + "air_network.xml";
		ScenarioImpl scenario = initScenario();
		if (this.useAirportCapacities){
			this.readAirportCapacities(scenarioData);
		}
		
		DgAirNetworkBuilder networkBuilder = new DgAirNetworkBuilder(scenario, transform, scenarioData);
		networkBuilder.createNetwork(flightsData, airports, outputNetworkFilename);
		Map<Id, SfMatsimAirport> airportMap = networkBuilder.getAirportMap();
		//
		DgTransitBuilder transitBuilder = new DgTransitBuilder(scenario);
		transitBuilder.createSchedule(flightsData, airportMap);
		transitBuilder.writeTransitFile(baseDirectory + FLIGHT_TRANSIT_SCHEDULE, baseDirectory + FLIGHT_TRANSIT_VEHICLES);
//
		writeConnectionData(baseDirectory, scenario, airports, flightsData, targetCrs);
		
	}

	public void createWorldFlightScenario(String inputOsmFilename,
			String inputOagFilename) throws Exception {
		String baseDirectory = dataBaseDirectory + "shared-svn/studies/countries/world/flight/" + flightScenarioDirectoryName;
		OutputDirectoryLogging.initLoggingWithOutputDirectory(baseDirectory);
		String oagFlightsFilename = baseDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;
		
		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		DgOagFlightsData flightsData = airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory, null, utcOffsetfile, oagFlightsFilename);
		Map<String, Coord> airports = airScheduleBuilder.getAirportCoordMap();

		createScenario(baseDirectory, flightsData, airports);
		
		OutputDirectoryLogging.closeOutputDirLogging();
	}	

	
	public void createEuropeanFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception{
		String baseDirectory = dataBaseDirectory + "shared-svn/studies/countries/eu/flight/" + flightScenarioDirectoryName;
		OutputDirectoryLogging.initLoggingWithOutputDirectory(baseDirectory);
		String oagFlightsFilename = baseDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;

		
		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		DgOagFlightsData flightsData = airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory,
				SfAirScheduleBuilder.EURO_COUNTRIES, utcOffsetfile, oagFlightsFilename);
		Map<String, Coord> airports = airScheduleBuilder.getAirportCoordMap();

		createScenario(baseDirectory, flightsData, airports);
		
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	
	public void createGermanFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception{
		String baseDirectory = dataBaseDirectory + "shared-svn/studies/countries/de/flight/" + flightScenarioDirectoryName;
		OutputDirectoryLogging.initLoggingWithOutputDirectory(baseDirectory);
		String oagFlightsFilename = baseDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;

		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		DgOagFlightsData flightsData = airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory,
				SfAirScheduleBuilder.GERMAN_COUNTRIES, utcOffsetfile, oagFlightsFilename);
		Map<String, Coord> airports = airScheduleBuilder.getAirportCoordMap();

		createScenario(baseDirectory, flightsData, airports);

		OutputDirectoryLogging.closeOutputDirLogging();
	}

	private void writeConnectionData(String baseDirectory, Scenario scenario, Map<String, Coord> airports,
			DgOagFlightsData flightsData, CoordinateReferenceSystem crs) {
		String shapeFileDirectoryname = baseDirectory + "shape_epsg_3395/";
		File shapeFileDirectory = new File(shapeFileDirectoryname);
		if (shapeFileDirectory.exists()){
			shapeFileDirectory.delete();
		}
		shapeFileDirectory.mkdir();
		
		DgNet2Shape.writeNetwork2Shape(scenario.getNetwork(), crs, shapeFileDirectoryname + SfAirNetworkBuilder.NETWORK_FILENAME + ".shp");
	}

}
