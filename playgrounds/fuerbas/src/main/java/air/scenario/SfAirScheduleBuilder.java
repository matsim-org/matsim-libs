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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.xml.sax.SAXException;

/**
 * @author sfuerbas
 * @author dgrether
 */
public class SfAirScheduleBuilder {

	private static final Logger log = Logger.getLogger(SfAirScheduleBuilder.class);

	public final static String[] EURO_COUNTRIES = { "AD", "AL", "AM", "AT", "AX", "AZ", "BA", "BE",
			"BG", "BY", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FO", "FR", "GB", "GI", "GE",
			"GG", "GR", "HR", "HU", "IE", "IM", "IS", "IT", "JE", "KZ", "LI", "LT", "LU", "LV", "MC",
			"MD", "ME", "MK", "MT", "NL", "NO", "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SJ", "SK",
			"SM", "TR", "UA", "VA" };

	public final static String[] GERMAN_COUNTRIES = { "DE" };

	public static final String AIRPORTS_FROM_OSM_OUTPUT_FILE = "osm_airports.txt";

	public static final String OAG_FLIGHTS_OUTPUT_FILENAME = "oag_flights.txt";

	private static final String missingAirportsOutputFilename = "missing_airports.txt";

	public static final String CITY_PAIRS_OUTPUT_FILENAME = "city_pairs.txt";
	
	public static final String UTC_OFFSET_FILE = "utc_offset.txt";

	protected Map<String, Coord> airportsInOsm = new HashMap<String, Coord>();
	protected Map<String, Coord> airportsInOag = new HashMap<String, Coord>();
	protected Map<String, Double> routes = new HashMap<String, Double>();
	protected Map<String, Integer> missingAirports = new HashMap<String, Integer>();
	protected Map<String, Double> cityPairDistance = new HashMap<String, Double>();
	private Map<String, Integer> utcOffset = new HashMap<String, Integer>();
	private boolean utcFileInUse = false;

	public void filter(String inputOsmFilename, String inputOagFilename, String outputDirectory) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		this.filter(inputOsmFilename, inputOagFilename, outputDirectory, null, null);
	}
	
	public void filter(String inputOsmFilename, String inputOagFilename, String outputDirectory, String utcOffsetInputfile) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		this.filter(inputOsmFilename, inputOagFilename, outputDirectory, null, utcOffsetInputfile);
	}

	
	@SuppressWarnings("rawtypes")
	public void filter(String inputOsmFile, String inputOagFile, String outputDirectory,
			String[] countries, String utcOffsetInputfile) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		String outputOagFile = outputDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;

		SfOsmAerowayParser osmReader = new SfOsmAerowayParser(
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.WGS84));
		osmReader.parse(inputOsmFile);

		int counter = 0;

		this.airportsInOsm = osmReader.airports;
		
//		new UTC Offset functionality, work in progress!
		/**@todo check UTC offsets for errors **/ 
		if (utcOffsetInputfile!=null) {
			getUtcOffsetMap(utcOffsetInputfile);
			this.utcFileInUse = true;
		}
		
		BufferedReader br = new BufferedReader(new FileReader(new File(inputOagFile)));
		BufferedWriter bwOag = new BufferedWriter(new FileWriter(new File(outputOagFile)));

		Map<String, String> flights = new HashMap<String, String>();
		int lines = 0;

		while (br.ready()) {
			String oneLine = br.readLine();
			String[] lineEntries = new String[81];
			lineEntries = oneLine.split(",");

			if (lines > 0) {

				for (int jj = 0; jj < 81; jj++) {
					lineEntries[jj] = lineEntries[jj].replaceAll("\"", "");
				}

				String originCountry = lineEntries[6];
				String destinationCountry = lineEntries[9];
				boolean origin = false;
				boolean destination = false;
				if (countries != null) {
					for (int ii = 0; ii < countries.length; ii++) {
						if (originCountry.equalsIgnoreCase(countries[ii]))
							origin = true;
						if (destinationCountry.equalsIgnoreCase(countries[ii]))
							destination = true;
					}
				}
				else {
					origin = true;
					destination = true;
				}

				if (origin && destination) {

					if (lineEntries[47].contains("O") || lineEntries[43].equalsIgnoreCase("")) {



						String carrier = lineEntries[0];
						String flightNumber = lineEntries[1].replaceAll(" ", "0");
						String flightDesignator = carrier + flightNumber;

						String originAirport = lineEntries[4];
						String destinationAirport = lineEntries[7];
						String route = originAirport + "_" + destinationAirport;
						double flightDistance = Integer.parseInt(lineEntries[42]) * 1.609344; // statute miles to kilometers
						
						String hours = lineEntries[13].substring(0, 3);
						String minutes = lineEntries[13].substring(3);
						double durationMinutes = Double.parseDouble(minutes) * 60; // convert flight dur minutes into seconds
						double durationHours = Double.parseDouble(hours) * 3600;
						double duration = durationHours + durationMinutes;
						double departureInSec = Double.parseDouble(lineEntries[10].substring(2)) * 60
								+ Double.parseDouble(lineEntries[10].substring(0, 2)) * 3600;
//					Getting UTC Offset of current airport's time zone
//						worldwide version (gets offset from web service) 
//						System.out.println("Getting UTC offset for "+originAirport+" in coutry: "+originCountry);
//						double utcOffset = utcOffsetNew.getUtcOffset(this.airportsInOsm.get(originAirport));
						
//						Getting UTC Offset from separate file which need to be created with SfUtcOffset
						if (this.utcFileInUse) {
							double utcOffset = this.utcOffset.get(originAirport);
							departureInSec = departureInSec - utcOffset;
							System.out.println("UTC offset was calculated as: "+utcOffset);
						}

//						version for Europe ONLY (based on manually entered offsets, see below)
						if (this.utcFileInUse==false) {
							double utcOffset = getOffsetUTC(originCountry) * 3600;
							departureInSec = departureInSec - utcOffset;
						}
//					
						if (departureInSec < 0)
							departureInSec += 86400.0; // shifting flights with departure on previous day in UTC time +24 hours
						double stops = Double.parseDouble(lineEntries[15]);
						String fullRouting = lineEntries[40];
						
						this.missingAirports.put(originAirport, 1);
						this.missingAirports.put(destinationAirport, 1);

						String aircraftType = lineEntries[21];
						int seatsAvail = Integer.parseInt(lineEntries[23]);

						
						//some error correction code
						if (lineEntries[14].contains("2") && !flights.containsKey(flightDesignator)
								&& seatsAvail > 0 && !originAirport.equalsIgnoreCase(destinationAirport)
								&& this.airportsInOsm.containsKey(originAirport)
								&& this.airportsInOsm.containsKey(destinationAirport)
								&& !originAirport.equalsIgnoreCase("HAD")
								&& !destinationAirport.equalsIgnoreCase("HAD")
								&& !originAirport.equalsIgnoreCase("BAX")
								&& !destinationAirport.equalsIgnoreCase("BAX")
								&& !originAirport.equalsIgnoreCase("CER")
								&& !destinationAirport.equalsIgnoreCase("CER")
								&& !aircraftType.equalsIgnoreCase("BUS") && (stops < 1)
								&& (fullRouting.length() <= 6)) {

							if (!this.routes.containsKey(route)) {
								this.routes.put(route, duration);
							}

							this.cityPairDistance.put(route, flightDistance);

							if ((flightDistance * 1000 / duration) <= 40.)
								log.debug("too low speed :" + flightDesignator);

							bwOag.write(route + "\t" + // TransitRoute
									route + "_" + carrier + "\t" + // TransitLine
									flightDesignator + "\t" + // vehicleId
									departureInSec + "\t" + // departure time in seconds
									this.routes.get(route) + "\t" + // journey time in seconds
									aircraftType + "\t" + // aircraft type
									seatsAvail + "\t" + // seats avail
									flightDistance); // distance in km
							flights.put(flightDesignator, "");
							bwOag.newLine();
							counter++;
							this.airportsInOag.put(originAirport, this.airportsInOsm.get(originAirport));
							this.airportsInOag
									.put(destinationAirport, this.airportsInOsm.get(destinationAirport));
						}
					}
				}
			}

			lines++;

		}

		bwOag.flush();
		bwOag.close();

		// produce some more output

		String outputOsmFile = outputDirectory + AIRPORTS_FROM_OSM_OUTPUT_FILE;
		String outputMissingAirportsFile = outputDirectory + missingAirportsOutputFilename;
		String cityPairsFile = outputDirectory + CITY_PAIRS_OUTPUT_FILENAME;

		BufferedWriter bwOsm = new BufferedWriter(new FileWriter(new File(outputOsmFile)));
		BufferedWriter bwMissing = new BufferedWriter(new FileWriter(
				new File(outputMissingAirportsFile)));
		BufferedWriter bwcityPairs = new BufferedWriter(new FileWriter(new File(cityPairsFile)));

		Iterator it = this.airportsInOag.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			bwOsm.write(pairs.getKey().toString() + "\t" + osmReader.airports.get(pairs.getKey()).getX()
					+ "\t" + osmReader.airports.get(pairs.getKey()).getY());
			this.missingAirports.remove(pairs.getKey().toString());
			bwOsm.newLine();
		}

		Iterator it2 = this.missingAirports.entrySet().iterator();
		while (it2.hasNext()) {
			Map.Entry pairs = (Map.Entry) it2.next();
			bwMissing.write(pairs.getKey().toString());
			bwMissing.newLine();
		}

		Iterator it3 = this.routes.entrySet().iterator();
		while (it3.hasNext()) {
			Map.Entry pairs = (Map.Entry) it3.next();
			bwcityPairs.write(pairs.getKey().toString() + "\t"
					+ this.cityPairDistance.get(pairs.getKey().toString()) + "\t"
					+ this.routes.get(pairs.getKey().toString()));
			bwcityPairs.newLine();
		}

		log.info("Anzahl der Airports: " + this.airportsInOag.size());
		log.info("Anzahl der City Pairs: " + this.routes.size());
		log.info("Anzahl der Flüge: " + counter);
		log.info("Anzahl der fehlenden Airport: " + this.missingAirports.size());

		bwOsm.flush();
		bwOsm.close();
		bwMissing.flush();
		bwMissing.close();
		bwcityPairs.flush();
		bwcityPairs.close();
		br.close();

	}
	
	private void getUtcOffsetMap(String inputfile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(inputfile)));
		while (br.ready()) {
			String oneLine = br.readLine();
			String[] lineEntries = new String[2];
			lineEntries = oneLine.split("\t");
			this.utcOffset.put(lineEntries[0], Integer.parseInt(lineEntries[1]));
		}
		br.close();
	}

	private double getOffsetUTC(String originCountry) {

		if (originCountry.equalsIgnoreCase("AD"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AL"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AM"))
			return 5;
		else if (originCountry.equalsIgnoreCase("AT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AX"))
			return 3;
		else if (originCountry.equalsIgnoreCase("AZ"))
			return 5;
		else if (originCountry.equalsIgnoreCase("BA"))
			return 2;
		else if (originCountry.equalsIgnoreCase("BE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("BG"))
			return 3;
		else if (originCountry.equalsIgnoreCase("BY"))
			return 3;
		else if (originCountry.equalsIgnoreCase("CH"))
			return 2;
		else if (originCountry.equalsIgnoreCase("CY"))
			return 3;
		else if (originCountry.equalsIgnoreCase("CZ"))
			return 2;
		else if (originCountry.equalsIgnoreCase("DE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("DK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("EE"))
			return 3;
		else if (originCountry.equalsIgnoreCase("ES"))
			return 2;
		else if (originCountry.equalsIgnoreCase("FI"))
			return 3;
		else if (originCountry.equalsIgnoreCase("FO"))
			return 1;
		else if (originCountry.equalsIgnoreCase("FR"))
			return 2;
		else if (originCountry.equalsIgnoreCase("GB"))
			return 1;
		else if (originCountry.equalsIgnoreCase("GI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("GE"))
			return 4;
		else if (originCountry.equalsIgnoreCase("GG"))
			return 1;
		else if (originCountry.equalsIgnoreCase("GR"))
			return 3;
		else if (originCountry.equalsIgnoreCase("HR"))
			return 2;
		else if (originCountry.equalsIgnoreCase("HU"))
			return 2;
		else if (originCountry.equalsIgnoreCase("IE"))
			return 1;
		else if (originCountry.equalsIgnoreCase("IM"))
			return 1;
		else if (originCountry.equalsIgnoreCase("IS"))
			return 0;
		else if (originCountry.equalsIgnoreCase("IT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("JE"))
			return 1;
		else if (originCountry.equalsIgnoreCase("KZ"))
			return 6;
		else if (originCountry.equalsIgnoreCase("LI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("LT"))
			return 3;
		else if (originCountry.equalsIgnoreCase("LU"))
			return 2;
		else if (originCountry.equalsIgnoreCase("LV"))
			return 3;
		else if (originCountry.equalsIgnoreCase("MC"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MD"))
			return 3;
		else if (originCountry.equalsIgnoreCase("ME"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("NL"))
			return 2;
		else if (originCountry.equalsIgnoreCase("NO"))
			return 2;
		else if (originCountry.equalsIgnoreCase("PL"))
			return 2;
		// Azores are UTC, while mainland and Madeira are UTC+1
		else if (originCountry.equalsIgnoreCase("PT"))
			return 1;
		else if (originCountry.equalsIgnoreCase("RO"))
			return 3;
		else if (originCountry.equalsIgnoreCase("RS"))
			return 0;
		// Russia with Moscow time zone offset UTC+4
		else if (originCountry.equalsIgnoreCase("RU"))
			return 4;
		else if (originCountry.equalsIgnoreCase("SE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SJ"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SM"))
			return 2;
		else if (originCountry.equalsIgnoreCase("TR"))
			return 3;
		else if (originCountry.equalsIgnoreCase("UA"))
			return 3;
		else if (originCountry.equalsIgnoreCase("VA"))
			return 2;

		throw new RuntimeException("No UTC offset for country " + originCountry
				+ " found in lookup table. Please add offset first!");
	}

	public static void main(String[] args) throws IOException, SAXException,
			ParserConfigurationException, InterruptedException {

		SfAirScheduleBuilder builder = new SfAirScheduleBuilder();

		String osmFile = "/home/dgrether/shared-svn/projects/throughFlightData/osm_daten/2010-12-28_aeroway_nodes.osm";
		String oagFile = "/media/data/work/repos/"
				+ "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";
		String outputDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/eu/flight/sf_oag_flight_model/";

		builder.filter(osmFile, oagFile, outputDirectory, EURO_COUNTRIES, UTC_OFFSET_FILE);

		// GERMAN AIR TRAFFIC ONLY BELOW

		outputDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/de/flight/sf_oag_flight_model/";
		builder = new SfAirScheduleBuilder();
		builder.filter(osmFile, oagFile, outputDirectory, GERMAN_COUNTRIES, UTC_OFFSET_FILE);

	}


}
