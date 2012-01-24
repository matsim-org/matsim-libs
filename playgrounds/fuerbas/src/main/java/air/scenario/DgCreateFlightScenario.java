/* *********************************************************************** *
 * project: org.matsim.*
 * CreateScenario
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import air.analysis.DgNet2Shape;


/**
 * @author dgrether
 *
 */
public class DgCreateFlightScenario {
	
	public static void createWorldFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception {
		String baseDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/world/flight/sf_oag_flight_model/";

		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory);

		String outputNetworkFilename = baseDirectory + "air_network.xml";
		String outputOsmAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_FROM_OSM_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputOsmAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

		SfTransitBuilder transitBuilder = new SfTransitBuilder();
		String inputNetworkFile = baseDirectory + SfAirNetworkBuilder.NETWORK_FILENAME;
		transitBuilder.createSchedule(baseDirectory + SfAirScheduleBuilder.OAG_FLIGHTS_OUTPUT_FILENAME, inputNetworkFile, baseDirectory);
	
		writeShape(baseDirectory, inputNetworkFile);
	}
	
	public static void createEuropeanFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception{
		String baseDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/eu/flight/sf_oag_flight_model/";

		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory, SfAirScheduleBuilder.EURO_COUNTRIES, SfAirScheduleBuilder.UTC_OFFSET_FILE);

		String outputNetworkFilename = baseDirectory + "air_network.xml";
		String outputOsmAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_FROM_OSM_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputOsmAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

		SfTransitBuilder transitBuilder = new SfTransitBuilder();
		String inputNetworkFile = baseDirectory + SfAirNetworkBuilder.NETWORK_FILENAME;
		transitBuilder.createSchedule(baseDirectory + SfAirScheduleBuilder.OAG_FLIGHTS_OUTPUT_FILENAME, inputNetworkFile, baseDirectory);

		writeShape(baseDirectory, inputNetworkFile);
	}
	
	public static void createGermanFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception{
		String baseDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/de/flight/sf_oag_flight_model/";
		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory, SfAirScheduleBuilder.GERMAN_COUNTRIES, SfAirScheduleBuilder.UTC_OFFSET_FILE);

		String outputNetworkFilename = baseDirectory + "air_network.xml";
		String outputOsmAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_FROM_OSM_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputOsmAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

		SfTransitBuilder transitBuilder = new SfTransitBuilder();
		String inputNetworkFile = baseDirectory + SfAirNetworkBuilder.NETWORK_FILENAME;
		transitBuilder.createSchedule(baseDirectory + SfAirScheduleBuilder.OAG_FLIGHTS_OUTPUT_FILENAME, inputNetworkFile, baseDirectory);
	
		writeShape(baseDirectory, inputNetworkFile);
	}

	public static void writeShape(String baseDirectory, String networkFilename){
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
		String inputOsmFilename = "/home/dgrether/shared-svn/projects/throughFlightData/osm_daten/2010-12-28_aeroway_nodes.osm";
		String inputOagFilename = "/media/data/work/repos/"
				+ "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";

		//WORLD WIDE AIR TRAFFIC
//		createWorldFlightScenario(inputOsmFilename, inputOagFilename);
		
		//EUROPEAN AIR TRAFFIC
		createEuropeanFlightScenario(inputOsmFilename, inputOagFilename);
		
		// GERMAN AIR TRAFFIC
		createGermanFlightScenario(inputOsmFilename, inputOagFilename);
		
	}

}
