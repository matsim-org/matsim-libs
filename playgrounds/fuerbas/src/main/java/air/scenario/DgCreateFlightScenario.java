/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
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
import java.util.HashMap;
import java.util.Map;

import air.analysis.DgNet2Shape;

/**
 * @author dgrether, sfuerbas
 *
 */

public class DgCreateFlightScenario {
	
	public static enum Direction {INBOUND, OUTBOUND, BOTH};
	
	public static final Map<String, Direction> filter = new HashMap<String, Direction>();
	static {
		filter.put("FRA", Direction.BOTH);
		filter.put("TXL", Direction.INBOUND);
		filter.put("MUC", Direction.OUTBOUND);
		filter.put("JFK", Direction.BOTH);
	}
	
	public static final DgStarinfo DEFAULTSTAR = new DgStarinfo("default", 5000.0, 1.0, 10.0);
	
	public static final Map<String, DgStarinfo> stars = new HashMap<String, DgStarinfo>();
	static {
		stars.put("default", DEFAULTSTAR);
	}
	
	public static final Map<String, Double> STARoffset = new HashMap<String, Double>();
	static {
		STARoffset.put("default", 10.);
		STARoffset.put("MUC", 17.4*60);
	}
	
	public static void createWorldFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception {
//		String baseDirectory = "/media/data/work/repos/"
//				+ "shared-svn/studies/countries/world/flight/sf_oag_flight_model/";
//		String utcOffsetfile = "/media/data/work/repos/"
//		+ "shared-svn/studies/countries/world/flight/sf_oag_flight_model/utc_offsets.txt";
		
		String baseDirectory = "Z:\\WinHome\\shared-svn\\studies\\countries\\de\\flight\\sf_oag_flight_model\\munich\\flight_model_muc_outbound\\";
//		String baseDirectory = "Z:\\WinHome\\flight_model_muc_all_flights\\";
		String utcOffsetfile = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\utc_offsets.txt";
		
		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory, utcOffsetfile);

		String outputNetworkFilename = baseDirectory + "air_network.xml";
		String outputAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

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
		String outputOsmAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputOsmAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

		SfTransitBuilder transitBuilder = new SfTransitBuilder();
		String inputNetworkFile = baseDirectory + SfAirNetworkBuilder.NETWORK_FILENAME;
		transitBuilder.createSchedule(baseDirectory + SfAirScheduleBuilder.OAG_FLIGHTS_OUTPUT_FILENAME, inputNetworkFile, baseDirectory);

		writeShape(baseDirectory, inputNetworkFile);
	}
	
	public static void createGermanFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception{
		
		String baseDirectory = "Z:\\WinHome\\shared-svn\\studies\\countries\\de\\flight\\sf_oag_flight_model\\";
		String utcOffsetfile = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\utc_offsets.txt";
		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		airScheduleBuilder.filter(inputOsmFilename, inputOagFilename, baseDirectory, SfAirScheduleBuilder.GERMAN_COUNTRIES, utcOffsetfile);

		String outputNetworkFilename = baseDirectory + "air_network.xml";
		String outputOsmAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputOsmAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

		SfTransitBuilder transitBuilder = new SfTransitBuilder();
		String inputNetworkFile = baseDirectory + SfAirNetworkBuilder.NETWORK_FILENAME;
		transitBuilder.createSchedule(baseDirectory + SfAirScheduleBuilder.OAG_FLIGHTS_OUTPUT_FILENAME, inputNetworkFile, baseDirectory);
	
		writeShape(baseDirectory, inputNetworkFile);
	}

	public static void writeShape(String baseDirectory, String networkFilename){
		
		String shapeFileDirectoryname = baseDirectory + "shape_epsg_3395\\"; //for windows file systems
//		String shapeFileDirectoryname = baseDirectory + "shape_epsg_3395/";

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
//		String inputOsmFilename = "/home/dgrether/shared-svn/projects/throughFlightData/osm_daten/2010-12-28_aeroway_nodes.osm";
//		String inputOagFilename = "/media/data/work/repos/"
//				+ "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";
		
		String inputAirportsFilename = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\worldwide_airports_with_coords.csv";
		String inputOagFilename = "Z:\\WinHome\\shared-svn\\projects\\throughFlightData\\oag_rohdaten\\OAGSEP09.CSV";

		//WORLD WIDE AIR TRAFFIC
		createWorldFlightScenario(inputAirportsFilename, inputOagFilename);
		
		//EUROPEAN AIR TRAFFIC
//		createEuropeanFlightScenario(inputOsmFilename, inputOagFilename);
		
		// GERMAN AIR TRAFFIC
//		createGermanFlightScenario(inputAirportsFilename, inputOagFilename);
		
	}

}
