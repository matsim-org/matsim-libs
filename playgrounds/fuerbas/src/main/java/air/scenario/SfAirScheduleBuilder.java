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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

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

	public static final String AIRPORTS_OUTPUT_FILE = "airports.txt";

	public static final String CITY_PAIRS_OUTPUT_FILENAME = "city_pairs.txt";

	public static final String UTC_OFFSET_FILE = "utc_offsets.txt";
	
	private static final String MISSING_AIRPORTS = "missing_airports.txt";
	
	private int filteredDueToCountry = 0;

	protected Map<String, Coord> availableAirportCoordinates = new HashMap<String, Coord>();
	protected Map<String, Coord> airportsInModel = new HashMap<String, Coord>();
	protected Map<String, Double> routeDurationMap = new HashMap<String, Double>();
	protected Map<String, Double> cityPairDistance = new HashMap<String, Double>();
	private Map<String, Double> utcOffset = new HashMap<String, Double>();
	private Set<String> missingAirportCodes = new HashSet<String>();
	

	@SuppressWarnings("rawtypes")
	public DgOagFlightsData filter(String inputAirportListFile, String inputOagFile, String outputDirectory,
			String[] countries, String utcOffsetInputfile, String oagFlightsOutputFilename) throws Exception {
		String outputOagFile = oagFlightsOutputFilename;
		DgOagFlightsData data = new DgOagFlightsData();
		this.loadAirportList(inputAirportListFile);

		this.loadUtcOffsets(utcOffsetInputfile);

		int counter = 0;
		BufferedReader br = new BufferedReader(new FileReader(new File(inputOagFile)));
		BufferedWriter bwOag = new BufferedWriter(new FileWriter(new File(outputOagFile)));

		Map<String, String> flights = new HashMap<String, String>();
		int lines = 0;

		while (br.ready()) {
			String oneLine = br.readLine();
			lines++;
			String[] lineEntries = new String[81];
			lineEntries = oneLine.split(",");
			if (lines > 1) {
				for (int jj = 0; jj < 81; jj++) {
					lineEntries[jj] = lineEntries[jj].replaceAll("\"", "");
				}


				String originCountry = lineEntries[6];
				String destinationCountry = lineEntries[9];
				String originAirport = lineEntries[4];
				String destinationAirport = lineEntries[7];


				
				if (countries != null) {
					if (! ( checkCountry(originCountry, countries) || checkCountry(destinationCountry, countries)) ) {// either origin country or destination country
//						log.info("Neither origin country: " + originCountry + " nor destination country: " + destinationCountry + " matches filter.");
//						log.info("skipping flight from " + originAirport + " to " + destinationAirport);
						filteredDueToCountry++;
						continue;
					}
				}

				if (  ! this.availableAirportCoordinates.containsKey(originAirport)) {
					log.warn("No coordinates for Airport: " + originAirport);
					this.missingAirportCodes.add(originAirport);
					continue;
				}
				if ( ! this.availableAirportCoordinates.containsKey(destinationAirport)) {
					log.warn("No coordinates for Airport: " + destinationAirport);
					this.missingAirportCodes.add(destinationAirport);
					continue;
				}

				
				if (! (this.utcOffset.containsKey(originAirport) && this.utcOffset.containsKey(destinationAirport))) {
					log.warn("No UTC Offset found for airport " + originAirport + " or " + destinationAirport);
					continue;
				}

		
					// filter codeshare flights (see WW_DBF_With_Frequency.DOC from OAG input data)
					// either "operating marker" set, "shared airline designator" not set or "duplicate" not set
					if (lineEntries[47].contains("O") || lineEntries[43].equalsIgnoreCase("")
							|| lineEntries[49].equalsIgnoreCase("")) {
						String carrier = lineEntries[0];
						String flightNumber = lineEntries[1].replaceAll(" ", "0");
						String flightDesignator = carrier + flightNumber;

						String route = originAirport + "_" + destinationAirport;
//						log.debug("route:  " + route);
						double flightDistance = Integer.parseInt(lineEntries[42]) * 1.609344; // statute miles to kilometers

						String hours = lineEntries[13].substring(0, 3);
						String minutes = lineEntries[13].substring(3);
						double durationMinutes = Double.parseDouble(minutes) * 60; // convert flight dur minutes into seconds
						double durationHours = Double.parseDouble(hours) * 3600;
						double duration = durationHours + durationMinutes;
						double departureInSec = Double.parseDouble(lineEntries[10].substring(2)) * 60
								+ Double.parseDouble(lineEntries[10].substring(0, 2)) * 3600;

						// Getting UTC Offset from separate file which need to be created with SfUtcOffset
//						log.debug("originAirport: " + originAirport);
						double utcOffset = this.utcOffset.get(originAirport);
						departureInSec = departureInSec - utcOffset;

						if (departureInSec < 0) {
							departureInSec += (24.0 * 3600.0); // shifting flights with departure on previous day in UTC time +24 hours
						}
						double stops = Double.parseDouble(lineEntries[15]);
						String fullRouting = lineEntries[40];

						String aircraftType = lineEntries[21];
						int seatsAvail = Integer.parseInt(lineEntries[23]);


						
						String daysOfOperation = lineEntries[14];
						daysOfOperation = daysOfOperation.replace(" ", "");
						char[] opsDays = daysOfOperation.toCharArray();
						// filtering days of operation, bus and train segment, flights with stops and duration of zero
						if ( 
//								first two filters currently applied further down
//								DgCreateDgFlightScenario.useSingleDayOfOperation
//								&& daysOfOperation.contains("2") && //filter for Tuesday flights only
								 !flights.containsKey(flightDesignator)
								&& seatsAvail > 0 // filter for flights with 1 PAX or more only
								&& !originAirport.equalsIgnoreCase(destinationAirport)
								&& !aircraftType.equalsIgnoreCase("BUS") // filter busses
								&& !aircraftType.equalsIgnoreCase("RFS") // filter bus/train
								&& !aircraftType.equalsIgnoreCase("TRN") // filter trains
								&& (stops < 1)
								&& (duration > 0.)
								&& (fullRouting.length() <= 6) ) {
								
							if  // filter for desired airports that may be set in DgCreateFlightScenario
								(  (! DgCreateSfFlightScenario.doApplyFilter )
										|| ( DgCreateSfFlightScenario.filter.containsKey(destinationAirport) && DgCreateSfFlightScenario.filter.get(destinationAirport).equals(DgCreateSfFlightScenario.Direction.INBOUND))
										|| ( DgCreateSfFlightScenario.filter.containsKey(originAirport) && DgCreateSfFlightScenario.filter.get(originAirport).equals(DgCreateSfFlightScenario.Direction.OUTBOUND))
										|| ( DgCreateSfFlightScenario.filter.containsKey(originAirport) && DgCreateSfFlightScenario.filter.get(originAirport).equals(DgCreateSfFlightScenario.Direction.BOTH))
										|| ( DgCreateSfFlightScenario.filter.containsKey(destinationAirport)	&& DgCreateSfFlightScenario.filter.get(destinationAirport).equals(DgCreateSfFlightScenario.Direction.BOTH)) 
									)
						 { 

								if (duration > 24.0 * 3600){
									log.warn("Flight " + flightNumber + " has a duration of " + Time.writeTime(duration) + " hh:mm:ss that is considered as not realistic, substracting 24 h...");
									duration -= (24.0 * 3600.0);
								}

								
								if (DgCreateSfFlightScenario.doCreateStars) {
							// desired values for STARs can be defined in DgCreateFlightScenario, otherwise default values will be
								// used

								if (DgCreateSfFlightScenario.STARoffset.containsKey(destinationAirport))
									duration = duration - (DgCreateSfFlightScenario.STARoffset.get(destinationAirport));
								else
									duration = duration - DgCreateSfFlightScenario.STARoffset.get("default");
							}

							if (!this.routeDurationMap.containsKey(route)) {
								this.routeDurationMap.put(route, duration);
							}

							this.cityPairDistance.put(route, flightDistance);

							if ((flightDistance * 1000 / duration) <= 40.)
								log.debug("too low speed :" + flightDesignator);

//							used to generate Tuesday flights only, for other days change the daysOfOperation filter below to desired day
							for (int dayCount=0; dayCount<=opsDays.length; dayCount++) {
								if (DgCreateDgFlightScenario.useSingleDayOfOperation && daysOfOperation.contains("2")) 	{					
									bwOag.write(route + "\t" + // TransitRoute
											route + "_" + carrier + "\t" + // TransitLine
											flightDesignator + "\t" + // vehicleId
											departureInSec + "\t" + // departure time in seconds
											duration + "\t" + // journey time in seconds
											aircraftType + "\t" + // aircraft type
											seatsAvail + "\t" + // seats avail
											flightDistance); // distance in km
									DgOagFlight dgf = new DgOagFlight(flightDesignator);
									dgf.setAircraftType(aircraftType);
									dgf.setCarrier(carrier);
									dgf.setDepartureTime(departureInSec);
									dgf.setDuration(duration);
									dgf.setAircraftType(aircraftType);
									dgf.setSeatsAvailable(seatsAvail);
									dgf.setRoute(route);
									dgf.setDistanceKm(flightDistance);
									dgf.setOriginCode(originAirport);
									dgf.setDestinationCode(destinationAirport);
									data.addFlight(dgf);
									
									flights.put(flightDesignator, "");
									bwOag.newLine();
									counter++;
									this.airportsInModel.put(originAirport, this.availableAirportCoordinates.get(originAirport));
									this.airportsInModel.put(destinationAirport, this.availableAirportCoordinates.get(destinationAirport));
							}
//								used to generate air traffic of an entire week with departures being shifted 24 hours for each day
								else if (!DgCreateDgFlightScenario.useSingleDayOfOperation) {
									flightDesignator = flightDesignator+"_"+opsDays[dayCount];
									int opsDay = Integer.parseInt(String.valueOf(opsDays[dayCount]));
									departureInSec = (departureInSec + opsDay * 24 * 3600. ) - 24 * 3600.0;
									bwOag.write(route + "\t" + // TransitRoute
											route + "_" + carrier + "\t" + // TransitLine
											flightDesignator + "\t" + // vehicleId
											departureInSec + "\t" + // departure time in seconds
											duration + "\t" + // journey time in seconds
											aircraftType + "\t" + // aircraft type
											seatsAvail + "\t" + // seats avail
											flightDistance); // distance in km
									DgOagFlight dgf = new DgOagFlight(flightDesignator);
									dgf.setAircraftType(aircraftType);
									dgf.setCarrier(carrier);
									dgf.setDepartureTime(departureInSec);
									dgf.setDuration(duration);
									dgf.setAircraftType(aircraftType);
									dgf.setSeatsAvailable(seatsAvail);
									dgf.setRoute(route);
									dgf.setDistanceKm(flightDistance);
									dgf.setOriginCode(originAirport);
									dgf.setDestinationCode(destinationAirport);
									data.addFlight(dgf);
									
									flights.put(flightDesignator, "");
									bwOag.newLine();
									counter++;
									this.airportsInModel.put(originAirport, this.availableAirportCoordinates.get(originAirport));
									this.airportsInModel.put(destinationAirport, this.availableAirportCoordinates.get(destinationAirport));
								}
							}
					 }
					}
					}
			}
			if (lines % 10000 == 0){
				log.info("Read " + lines +  " lines of oag data...");
			}
		}

		bwOag.flush();
		bwOag.close();
		br.close();

		// produce some more output
		this.writeAirportsInModel(outputDirectory);

		this.writeRouteDurations(outputDirectory);
		
		this.writeMissingAirports(outputDirectory);

		log.info("Anzahl der Airports: " + this.airportsInModel.size());
		log.info("Anzahl der Airports ohne Coordinaten: " + this.missingAirportCodes.size());
		log.info("Anzahl der City Pairs: " + this.routeDurationMap.size());
		log.info("Anzahl der Flüge: " + counter);
		log.info("Anzahl der Zeilen die durch den Länderfilter gefiltert wurden: " + this.filteredDueToCountry);
		log.info("Anzahl der Zeilen mit Flügen: " + (lines - 1));
		return data;
	}

	private void writeRouteDurations(String outputDirectory) throws  IOException {
		String cityPairsFile = outputDirectory + CITY_PAIRS_OUTPUT_FILENAME;
		BufferedWriter bwcityPairs = new BufferedWriter(new FileWriter(new File(cityPairsFile)));
		for (Entry<String, Double> pairs : this.routeDurationMap.entrySet()){
			bwcityPairs.write(pairs.getKey().toString() + "\t"
					+ this.cityPairDistance.get(pairs.getKey().toString()) + "\t"
					+ this.routeDurationMap.get(pairs.getKey().toString()));
			bwcityPairs.newLine();
		}
		bwcityPairs.flush();
		bwcityPairs.close();
	}
	
	
	private void writeMissingAirports(String outputDirectory) throws  IOException {
		String filename = outputDirectory + MISSING_AIRPORTS;
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		bw.write("Airports without coordinates");
		bw.newLine();
		for (String s : this.missingAirportCodes){
			bw.write(s);
			bw.newLine();
		}
		bw.close();
	}
	
	private void writeAirportsInModel(String outputDirectory) throws  IOException {
		String outputAirportFile = outputDirectory + AIRPORTS_OUTPUT_FILE;
		BufferedWriter bwOsm = new BufferedWriter(new FileWriter(new File(outputAirportFile)));
		for (Entry<String, Coord> pairs : this.airportsInModel.entrySet()) {
			bwOsm.write(pairs.getKey().toString() + "\t" + this.airportsInModel.get(pairs.getKey()).getX()
					+ "\t" + this.airportsInModel.get(pairs.getKey()).getY());
			bwOsm.newLine();
		}
		bwOsm.flush();
		bwOsm.close();
	}
	

	private static boolean checkCountry(String originCountry, String[] countries) {
		for (int i = 0; i < countries.length; i++){
			if (countries[i].equalsIgnoreCase(originCountry)){
				return true;
			}
		}
		return false;
	}


	private void getUtcOffsetMap(String inputfile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(inputfile)));
		while (br.ready()) {
			String oneLine = br.readLine();
			String[] lineEntries = new String[2];
			lineEntries = oneLine.split("\t");
			this.utcOffset.put(lineEntries[0], Double.parseDouble(lineEntries[1]));
		}
		br.close();
	}

	private void loadAirportList(String inputfile) throws Exception {
		BufferedReader brAirports = new BufferedReader(new FileReader(new File(inputfile)));
		while (brAirports.ready()) {
			String line = brAirports.readLine();
			String[] entries = line.split("\t");
			String airportCode = entries[0];
			String xCoord = entries[1];
			String yCoord = entries[2];
			this.availableAirportCoordinates.put(airportCode, new CoordImpl(xCoord, yCoord));
		}
		brAirports.close();
	}

	private void loadUtcOffsets(String inputfile) throws Exception, IOException {
		log.debug("Loading utc offsets from file: " + inputfile);
		BufferedReader brUtc = new BufferedReader(new FileReader(new File(inputfile)));
		while (brUtc.ready()) {
			String line = brUtc.readLine();
			String[] entries = line.split("\t");
			String airportCode = entries[0];
			double offset = Double.parseDouble(entries[1]);
			this.utcOffset.put(airportCode, offset);
		}
		brUtc.close();
		log.debug("loaded " + this.utcOffset.size() + " utc offsets ");
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

	public static void main(String[] args) throws Exception {

		SfAirScheduleBuilder builder = new SfAirScheduleBuilder();

		String osmFile = "/home/dgrether/shared-svn/projects/throughFlightData/osm_daten/2010-12-28_aeroway_nodes.osm";
		String oagFile = "/media/data/work/repos/"
				+ "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";
		String outputDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/eu/flight/sf_oag_flight_model/";
		
//		builder.filter(osmFile, oagFile, outputDirectory, EURO_COUNTRIES, UTC_OFFSET_FILE);

		// GERMAN AIR TRAFFIC ONLY BELOW

		outputDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/de/flight/sf_oag_flight_model/";
		builder = new SfAirScheduleBuilder();
//		builder.filter(osmFile, oagFile, outputDirectory, GERMAN_COUNTRIES, UTC_OFFSET_FILE);

	}

	public Map<String, Coord> getAirportCoordMap() {
		return this.airportsInModel;
	}

}
