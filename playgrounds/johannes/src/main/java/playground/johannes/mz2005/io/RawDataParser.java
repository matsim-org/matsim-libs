/* *********************************************************************** *
 * project: org.matsim.*
 * TripParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.mz2005.io;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;


/**
 * @author illenberger
 * 
 */
public class RawDataParser {

	private static final Logger logger = Logger.getLogger(RawDataParser.class);

	private static final String SEPARATOR = "\t";

	public Map<String, PersonDataContainer> readPersons(String filename) throws IOException {
		Map<String, PersonDataContainer> persons = new HashMap<String, PersonDataContainer>();

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		String line = reader.readLine();
		TObjectIntHashMap<String> colNames = new TObjectIntHashMap<String>();
		String[] tokens = line.split(SEPARATOR);
		for (int i = 0; i < tokens.length; i++) {
			colNames.put(tokens[i].trim(), i);
		}

		while ((line = reader.readLine()) != null) {
			try {
				tokens = line.split(SEPARATOR);

				PersonDataContainer container = new PersonDataContainer();

				String householdId = tokens[colNames.get("HHNR")];
				String personId = tokens[colNames.get("ZIELPNR")];
				container.id = householdId + "." + personId;
				container.age = Integer.parseInt(tokens[colNames.get("F23B")]);
				container.referenceDay = Integer.parseInt(tokens[colNames.get("TAG")]);

				persons.put(container.id, container);

			} catch (Exception e) {
				logger.warn("Failed to parse person: " + e.getMessage());
			}
		}
		logger.info(String.format("Parsed %1$s persons.", persons.size()));

		return persons;
	}

	public void readTrips(String filename, Map<String, PersonDataContainer> persons) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		TObjectIntHashMap<String> colNames = readColNames(reader);

		int totalTrips = 0;
		int invalidTrips = 0;
		int outwardTrips = 0;

		TripDataContaienr container = null;

		String line = null;
		String[] tokens = null;
		while ((line = reader.readLine()) != null) {
			totalTrips++;
			try {
				tokens = line.split(SEPARATOR);

				container = new TripDataContaienr();

				String householdId = tokens[colNames.get("HHNR")];
				String personId = tokens[colNames.get("ZIELPNR")];
				container.personId = householdId + "." + personId;

				container.tripId = tokens[colNames.get("WEGNR")];
				container.distance = Double.parseDouble(tokens[colNames.get("w_dist_obj2")]);
				container.startTime = Integer.parseInt(tokens[colNames.get("F58")]);
				container.endTime = Integer.parseInt(tokens[colNames.get("F514")]);
				container.duration = Double.parseDouble(tokens[colNames.get("dauer1")]);

				container.mode = Integer.parseInt(tokens[colNames.get("wmittel")]);
				if (container.mode > 7 && container.mode < 14)
					container.aggrMode = LegMode.car;
				else if (container.mode > 1 && container.mode < 8)
					container.aggrMode = LegMode.pt;
				else
					container.aggrMode = LegMode.other;

				container.type = Integer.parseInt(tokens[colNames.get("wzweck1")]);

				String type2 = tokens[colNames.get("wzweck2")];
				if (type2.equals("1")) {
					outwardTrips++;
					container.outwardTrip = true;
				} else if (type2.equals("3"))
					container.roundTrip = true;

				container.accompanists = Integer.parseInt(tokens[colNames.get("F87")]);
				container.accompanists = Math.max(container.accompanists, 0);

				container.leisureType = Integer.parseInt(tokens[colNames.get("F5202")]);

				double x_start = Double.parseDouble(tokens[colNames.get("x_start")]);
				double y_start = Double.parseDouble(tokens[colNames.get("y_start")]);
				container.startCoord = new double[] { x_start, y_start };

				double x_dest = Double.parseDouble(tokens[colNames.get("x_dest")]);
				double y_dest = Double.parseDouble(tokens[colNames.get("y_dest")]);
				container.destCoord = new double[] { x_dest, y_dest };

				PersonDataContainer pContainer = persons.get(container.personId);
				if (pContainer != null) {
					pContainer.trips.add(container);
				} else {
					logger.warn(String.format("Person with id %1$s not found!", container.personId));
				}

			} catch (Exception e) {
				invalidTrips++;
			}
		}

		logger.info(String.format("Parsed %1$s trips, %2$s outward trips, %3$s invalid trips.", totalTrips,
				outwardTrips, invalidTrips));
	}

	public void readLegs(String filename, Map<String, PersonDataContainer> persons) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		TObjectIntHashMap<String> colNames = readColNames(reader);

		int total = 0;
		int invalid = 0;

		String line = null;
		String tokens[] = null;
		while ((line = reader.readLine()) != null) {
			try {
				tokens = line.split(SEPARATOR);

				LegDataContainer leg = new LegDataContainer();
				leg.id = tokens[colNames.get("ETNR")];
				leg.mode = Integer.parseInt(tokens[colNames.get("F510")]);

				String personId = tokens[colNames.get("HHNR")] + "." + tokens[colNames.get("ZIELPNR")];
				PersonDataContainer person = persons.get(personId);
				if (person != null) {
					String tripId = tokens[colNames.get("WEGNR")];
					TripDataContaienr trip = null;
					for (TripDataContaienr tc : person.trips) {
						if (tc.tripId.equals(tripId)) {
							trip = tc;
							break;
						}
					}
					if (trip != null) {
						trip.legs.add(leg);
					} else {
						invalid++;
						// logger.warn(String.format("Trip with id %1$s for person with id %2$s not found.",
						// tripId, personId));
					}
					total++;
				} else {
					logger.warn(String.format("Person with id %1$s not found.", personId));
				}

			} catch (Exception e) {
				logger.warn("Failed to parse leg: " + e.getMessage());
			}
		}

		logger.info(String.format("Parsed %1$s legs, %2$s invalid.", total, invalid));
	}

	private TObjectIntHashMap<String> readColNames(BufferedReader reader) throws IOException {
		String line = reader.readLine();
		TObjectIntHashMap<String> colNames = new TObjectIntHashMap<String>();
		String[] tokens = line.split(SEPARATOR);
		for (int i = 0; i < tokens.length; i++) {
			colNames.put(tokens[i].trim(), i);
		}
		return colNames;
	}
}
