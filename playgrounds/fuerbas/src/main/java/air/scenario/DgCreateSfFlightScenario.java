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

import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import air.analysis.DgNet2Shape;
import air.scenario.countryfilter.DgGermanyCountryFilter;

/**
 * @author dgrether, sfuerbas
 */

public class DgCreateSfFlightScenario {
	
	public static final boolean doApplyAirportFilter = false;
	public static enum Direction {INBOUND, OUTBOUND, BOTH};
	public static final Map<String, Direction> filter = new HashMap<String, Direction>();
	static {
		filter.put("MUC", Direction.BOTH);
	}
	
	public static final int NUMBER_OF_RUNWAYS = 2;
	
	
	public static final boolean doCreateStars = false;
	public static final DgStarinfo DEFAULTSTAR = new DgStarinfo("default", 100000.0, 1./60., 463./3.6);	//default speed below FL100: 250 knots = 463km/h 
	public static final Map<String, DgStarinfo> stars = new HashMap<String, DgStarinfo>();	
	static {
		stars.put("default", DEFAULTSTAR);
		stars.put("MUC", new DgStarinfo("MUC", 134270.0, 58/3600., 463./3.6));	// v = s/t    s = t*v 
	}
	
	public static final Map<String, Double> STARoffset = new HashMap<String, Double>();
	static {
		double defaultduration = DEFAULTSTAR.getLength()/DEFAULTSTAR.getFreespeed();
		STARoffset.put("default", defaultduration);
		STARoffset.put("MUC", stars.get("MUC").getLength()/stars.get("MUC").getFreespeed());
	}
	
	private static String utcOffsetfile = "/media/data/work/repos/shared-svn/studies/countries/world/flight/sf_oag_flight_model/utc_offsets.txt";
	private static String dataBaseDirectory = "/media/data/work/repos/";
	private static String inputAirportsFilename = dataBaseDirectory + "shared-svn/studies/countries/world/flight/sf_oag_flight_model/worldwide_airports_with_coords.csv";
	private static String inputOagFilename = dataBaseDirectory + "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";
//	private static String inputAirportsFilename = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\worldwide_airports_with_coords.csv";
//	private static String inputOagFilename = "Z:\\WinHome\\shared-svn\\projects\\throughFlightData\\oag_rohdaten\\OAGSEP09.CSV";
	private static final String OAG_FLIGHTS_OUTPUT_FILENAME = "oag_flights.txt";

	
	
	public static void createWorldFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception {
//		String baseDirectory = "/media/data/work/repos/"
//				+ "shared-svn/studies/countries/world/flight/sf_oag_flight_model/";
//		String utcOffsetfile = "/media/data/work/repos/"
//		+ "shared-svn/studies/countries/world/flight/sf_oag_flight_model/utc_offsets.txt";
		
		String baseDirectory = "/media/data/work/repos/shared-svn/studies/countries/world/flight/sf_oag_flight_model/";
		OutputDirectoryLogging.initLoggingWithOutputDirectory(baseDirectory);
		String oagFlightsFilename = baseDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;

		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		airScheduleBuilder.readDataAndFilter(inputOsmFilename, inputOagFilename, baseDirectory, utcOffsetfile, oagFlightsFilename);

		String outputNetworkFilename = baseDirectory + "air_network.xml";
		String outputAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

		SfTransitBuilder transitBuilder = new SfTransitBuilder();
		String inputNetworkFile = baseDirectory + SfAirNetworkBuilder.NETWORK_FILENAME;
		transitBuilder.createSchedule(oagFlightsFilename, inputNetworkFile, baseDirectory);
	
		writeShape(baseDirectory, inputNetworkFile);
		OutputDirectoryLogging.closeOutputDirLogging();
	}
	
	public static void createEuropeanFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception{
		String baseDirectory = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/sf_oag_flight_model/";
		OutputDirectoryLogging.initLoggingWithOutputDirectory(baseDirectory);
		String oagFlightsFilename = baseDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;

		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		airScheduleBuilder.setCountryFilter(new SfEuropeCountryFilter());
		airScheduleBuilder.readDataAndFilter(inputOsmFilename, inputOagFilename, baseDirectory, utcOffsetfile, oagFlightsFilename);

		String outputNetworkFilename = baseDirectory + "air_network.xml";
		String outputOsmAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputOsmAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

		SfTransitBuilder transitBuilder = new SfTransitBuilder();
		String inputNetworkFile = baseDirectory + SfAirNetworkBuilder.NETWORK_FILENAME;
		transitBuilder.createSchedule(oagFlightsFilename, inputNetworkFile, baseDirectory);

		writeShape(baseDirectory, inputNetworkFile);
		OutputDirectoryLogging.closeOutputDirLogging();
	}
	
	public static void createGermanFlightScenario(String inputOsmFilename, String inputOagFilename) throws Exception{
		
		String baseDirectory = "/media/data/work/repos/shared-svn/studies/countries/de/flight/sf_oag_flight_model/";
		OutputDirectoryLogging.initLoggingWithOutputDirectory(baseDirectory);
		String oagFlightsFilename = baseDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;
		
		SfAirScheduleBuilder airScheduleBuilder = new SfAirScheduleBuilder();
		airScheduleBuilder.setCountryFilter(new DgGermanyCountryFilter(false));
		airScheduleBuilder.readDataAndFilter(inputOsmFilename, inputOagFilename, baseDirectory,
				utcOffsetfile, oagFlightsFilename);

		String outputNetworkFilename = baseDirectory + "air_network.xml";
		String outputOsmAirportsFilename = baseDirectory + SfAirScheduleBuilder.AIRPORTS_OUTPUT_FILE;
		String outputCityPairsFilename = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		
		SfAirNetworkBuilder networkBuilder = new SfAirNetworkBuilder();
		networkBuilder.createNetwork(outputOsmAirportsFilename, outputCityPairsFilename, outputNetworkFilename);

		SfTransitBuilder transitBuilder = new SfTransitBuilder();
		String inputNetworkFile = baseDirectory + SfAirNetworkBuilder.NETWORK_FILENAME;
		transitBuilder.createSchedule(oagFlightsFilename, inputNetworkFile, baseDirectory);
	
		writeShape(baseDirectory, inputNetworkFile);
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	public static void writeShape(String baseDirectory, String networkFilename){
		
//		String shapeFileDirectoryname = baseDirectory + "shape_epsg_3395\\"; //for windows file systems
		String shapeFileDirectoryname = baseDirectory + "shape_epsg_3395/";
		File shapeFileDirectory = new File(shapeFileDirectoryname);
		if (shapeFileDirectory.exists()){
			shapeFileDirectory.delete();
		}
		shapeFileDirectory.mkdir();
		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:3395");
		DgNet2Shape.writeNetwork2Shape(networkFilename, crs, shapeFileDirectoryname + SfAirNetworkBuilder.NETWORK_FILENAME + ".shp");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//WORLD WIDE AIR TRAFFIC
//		createWorldFlightScenario(inputAirportsFilename, inputOagFilename);
		
		//EUROPEAN AIR TRAFFIC
//		createEuropeanFlightScenario(inputAirportsFilename, inputOagFilename);
		
		// GERMAN AIR TRAFFIC
		createGermanFlightScenario(inputAirportsFilename, inputOagFilename);
		
	}

}
