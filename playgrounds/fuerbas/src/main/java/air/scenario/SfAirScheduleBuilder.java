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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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

	private Map<String, Coord> airportsInModel = new HashMap<String, Coord>();
	private Map<String, Double> routeDurationMap = new HashMap<String, Double>();
	private Map<String, Double> cityPairDistance = new HashMap<String, Double>();
	private Map<String, Double> utcOffset = new HashMap<String, Double>();
	private Set<String> missingAirportCodes = new HashSet<String>();

	private String[] countryFilter = null;

	public void setCountryFilter(String[] countries) {
		this.countryFilter = countries;
	}

	@SuppressWarnings("rawtypes")
	public DgOagFlightsData readDataAndFilter(String inputAirportListFile, String inputOagFile,
			String outputDirectory, String[] countries, String utcOffsetInputfile,
			String oagFlightsOutputFilename) throws Exception {
		Map<String, Coord> availableAirportCoordinates = new DgAirportsReader()
				.loadAirportCoordinates(inputAirportListFile);
		Map<String, Double> utcOffset = new DgUTCOffsetsReader().loadUtcOffsets(utcOffsetInputfile);
		this.countryFilter = countries;
		List<DgOagLine> oagLines = new DgOagReader().readOagLines(inputOagFile);
		return this.filter(oagLines, outputDirectory, oagFlightsOutputFilename,
				availableAirportCoordinates, utcOffset);
	}

	private boolean airportCoordinatesAvailable(String originAirport, String destinationAirport,
			Map<String, Coord> availableAirportCoordinates) {
		if (!availableAirportCoordinates.containsKey(originAirport)) {
			log.warn("No coordinates for Airport: " + originAirport);
			this.missingAirportCodes.add(originAirport);
			return false;
		}
		if (!availableAirportCoordinates.containsKey(destinationAirport)) {
			log.warn("No coordinates for Airport: " + destinationAirport);
			this.missingAirportCodes.add(destinationAirport);
			return false;
		}
		return true;
	}

	private boolean isCountryOfInterest(String originCountry, String destinationCountry) {
		if (countryFilter != null) {
			if (!(checkCountry(originCountry, countryFilter) || checkCountry(destinationCountry,
					countryFilter))) {// either origin country or destination country
				// log.info("Neither origin country: " + originCountry + " nor destination country: " + destinationCountry +
				// " matches filter.");
				// log.info("skipping flight from " + originAirport + " to " + destinationAirport);
				filteredDueToCountry++;
				return false;
			}
		}
		return true;
	}

	private boolean isUTCOffsetAvailable(String originAirport, String destinationAirport) {
		if (!(this.utcOffset.containsKey(originAirport) && this.utcOffset
				.containsKey(destinationAirport))) {
			log.warn("No UTC Offset found for airport " + originAirport + " or " + destinationAirport);
			return false;
		}
		return true;
	}

	private boolean isCodeshareFlight(DgOagLine line) {
		return line.isCodeshareFlight();
	}

	public DgOagFlightsData filter(List<DgOagLine> oagLines, String outputDirectory,
			String oagFlightsOutputFilename, Map<String, Coord> availableAirportCoordinates,
			Map<String, Double> utcOffset2) throws Exception {
		DgOagFlightsData data = new DgOagFlightsData();
		int counter = 0;
		BufferedWriter bwOag = new BufferedWriter(new FileWriter(new File(oagFlightsOutputFilename)));

		Map<String, String> flights = new HashMap<String, String>();

		for (DgOagLine l : oagLines) {
			String originCountry = l.getOriginCountry();
			String destinationCountry = l.getDestinationCountry();
			String originAirport = l.getOriginAirport();
			String destinationAirport = l.getDestinationAirport();

			if (!isCountryOfInterest(originCountry, destinationCountry)) {
				continue;
			}
			if (!airportCoordinatesAvailable(originAirport, destinationAirport,
					availableAirportCoordinates)) {
				continue;
			}
			if (!isUTCOffsetAvailable(originAirport, destinationAirport)) {
				continue;
			}
			if (isCodeshareFlight(l)) {
				continue;
			}

			String carrier = l.getCarrier();
			String flightNumber = l.getFlightNumber();
			String flightDesignator = carrier + flightNumber;
			String route = originAirport + "_" + destinationAirport;
			// log.debug("route:  " + route);
			double flightDistance = l.getFlightDistanceMiles() * 1.609344; // statute miles to kilometers
			double duration = l.getHours() * 3600 + l.getMinutes() * 60;
			double departureInSec = l.getDepartureTimeSeconds();

			// Getting UTC Offset from separate file which need to be created with SfUtcOffset
			// log.debug("originAirport: " + originAirport);
			double utcOffset = this.utcOffset.get(originAirport);
			departureInSec = departureInSec - utcOffset;

			if (departureInSec < 0) {
				departureInSec += (24.0 * 3600.0); // shifting flights with departure on previous day in UTC time +24 hours
			}
			double stops = l.getStops();
			String fullRouting = l.getFullRouting();

			String aircraftType = l.getAircraftType();
			int seatsAvail = l.getSeatsAvailable();
			char[] opsDays = l.getDaysOfOperation();
			// filtering days of operation, bus and train segment, flights with stops and duration of zero
			if (
			// first two filters currently applied further down
			// DgCreateDgFlightScenario.useSingleDayOfOperation
			// && daysOfOperation.contains("2") && //filter for Tuesday flights only
			!flights.containsKey(flightDesignator)
					&& seatsAvail > 0 // filter for flights with 1 PAX or more only
					&& !originAirport.equalsIgnoreCase(destinationAirport)
					&& !aircraftType.equalsIgnoreCase("BUS") // filter busses
					&& !aircraftType.equalsIgnoreCase("RFS") // filter bus/train
					&& !aircraftType.equalsIgnoreCase("TRN") // filter trains
					&& (stops < 1) && (duration > 0.) && (fullRouting.length() <= 6)) {

				if // filter for desired airports that may be set in DgCreateFlightScenario
				((!DgCreateSfFlightScenario.doApplyFilter)
						|| (DgCreateSfFlightScenario.filter.containsKey(destinationAirport) && DgCreateSfFlightScenario.filter
								.get(destinationAirport).equals(DgCreateSfFlightScenario.Direction.INBOUND))
						|| (DgCreateSfFlightScenario.filter.containsKey(originAirport) && DgCreateSfFlightScenario.filter
								.get(originAirport).equals(DgCreateSfFlightScenario.Direction.OUTBOUND))
						|| (DgCreateSfFlightScenario.filter.containsKey(originAirport) && DgCreateSfFlightScenario.filter
								.get(originAirport).equals(DgCreateSfFlightScenario.Direction.BOTH))
						|| (DgCreateSfFlightScenario.filter.containsKey(destinationAirport) && DgCreateSfFlightScenario.filter
								.get(destinationAirport).equals(DgCreateSfFlightScenario.Direction.BOTH))) {

					if (duration > 24.0 * 3600) {
						log.warn("Flight " + flightNumber + " has a duration of " + Time.writeTime(duration)
								+ " hh:mm:ss that is considered as not realistic, substracting 24 h...");
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

					// used to generate Tuesday flights only, for other days change the daysOfOperation filter below to desired
					// day
					if (DgCreateDgFlightScenario.useSingleDayOfOperation && this.operatesTuesdays(opsDays)) {
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
						this.airportsInModel.put(originAirport, availableAirportCoordinates.get(originAirport));
						this.airportsInModel.put(destinationAirport,
								availableAirportCoordinates.get(destinationAirport));
					}
					// used to generate air traffic of an entire week with departures being shifted 24 hours for each day
					else if (!DgCreateDgFlightScenario.useSingleDayOfOperation) {
						for (int dayCount = 0; dayCount <= opsDays.length; dayCount++) {
							flightDesignator = flightDesignator + "_" + opsDays[dayCount];
							int opsDay = Integer.parseInt(String.valueOf(opsDays[dayCount]));
							departureInSec = (departureInSec + opsDay * 24 * 3600.) - 24 * 3600.0;
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
							this.airportsInModel.put(originAirport,
									availableAirportCoordinates.get(originAirport));
							this.airportsInModel.put(destinationAirport,
									availableAirportCoordinates.get(destinationAirport));
						}
					}
				}
			}

		}

		bwOag.flush();
		bwOag.close();

		// produce some more output
		this.writeAirportsInModel(outputDirectory);

		this.writeRouteDurations(outputDirectory);

		this.writeMissingAirports(outputDirectory);

		log.info("Anzahl der Airports: " + this.airportsInModel.size());
		log.info("Anzahl der Airports ohne Coordinaten: " + this.missingAirportCodes.size());
		log.info("Anzahl der City Pairs: " + this.routeDurationMap.size());
		log.info("Anzahl der Flüge: " + counter);
		log.info("Anzahl der Zeilen die durch den Länderfilter gefiltert wurden: "
				+ this.filteredDueToCountry);
		return data;
	}

	private void writeRouteDurations(String outputDirectory) throws IOException {
		String cityPairsFile = outputDirectory + CITY_PAIRS_OUTPUT_FILENAME;
		BufferedWriter bwcityPairs = new BufferedWriter(new FileWriter(new File(cityPairsFile)));
		for (Entry<String, Double> pairs : this.routeDurationMap.entrySet()) {
			bwcityPairs.write(pairs.getKey().toString() + "\t"
					+ this.cityPairDistance.get(pairs.getKey().toString()) + "\t"
					+ this.routeDurationMap.get(pairs.getKey().toString()));
			bwcityPairs.newLine();
		}
		bwcityPairs.flush();
		bwcityPairs.close();
	}

	private boolean operatesTuesdays(char[] opsDays) {
		for (char c : opsDays) {
			if ("2".equals(c)) {
				return true;
			}
		}
		return false;
	}

	private void writeMissingAirports(String outputDirectory) throws IOException {
		String filename = outputDirectory + MISSING_AIRPORTS;
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		bw.write("Airports without coordinates");
		bw.newLine();
		for (String s : this.missingAirportCodes) {
			bw.write(s);
			bw.newLine();
		}
		bw.close();
	}

	private void writeAirportsInModel(String outputDirectory) throws IOException {
		String outputAirportFile = outputDirectory + AIRPORTS_OUTPUT_FILE;
		BufferedWriter bwOsm = new BufferedWriter(new FileWriter(new File(outputAirportFile)));
		for (Entry<String, Coord> pairs : this.airportsInModel.entrySet()) {
			bwOsm.write(pairs.getKey().toString() + "\t"
					+ this.airportsInModel.get(pairs.getKey()).getX() + "\t"
					+ this.airportsInModel.get(pairs.getKey()).getY());
			bwOsm.newLine();
		}
		bwOsm.flush();
		bwOsm.close();
	}

	private static boolean checkCountry(String originCountry, String[] countries) {
		for (int i = 0; i < countries.length; i++) {
			if (countries[i].equalsIgnoreCase(originCountry)) {
				return true;
			}
		}
		return false;
	}

	public Map<String, Coord> getAirportCoordMap() {
		return this.airportsInModel;
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

	public static void main(String[] args) throws Exception {

		SfAirScheduleBuilder builder = new SfAirScheduleBuilder();

		String osmFile = "/home/dgrether/shared-svn/projects/throughFlightData/osm_daten/2010-12-28_aeroway_nodes.osm";
		String oagFile = "/media/data/work/repos/"
				+ "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";
		String outputDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/eu/flight/sf_oag_flight_model/";

		// builder.filter(osmFile, oagFile, outputDirectory, EURO_COUNTRIES, UTC_OFFSET_FILE);

		// GERMAN AIR TRAFFIC ONLY BELOW

		outputDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/de/flight/sf_oag_flight_model/";
		builder = new SfAirScheduleBuilder();
		// builder.filter(osmFile, oagFile, outputDirectory, GERMAN_COUNTRIES, UTC_OFFSET_FILE);

	}

}
