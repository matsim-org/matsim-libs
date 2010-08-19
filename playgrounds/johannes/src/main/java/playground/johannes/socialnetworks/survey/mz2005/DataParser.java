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
package playground.johannes.socialnetworks.survey.mz2005;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.math.Distribution;


/**
 * @author illenberger
 * 
 */
public class DataParser {

	private static final Logger logger = Logger.getLogger(DataParser.class);
	
	private static final String SEPARATOR = "\t";

//	private static GoogleLocationLookup googleLookup;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String basedir = "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/rawdata/";
		DataParser parser = new DataParser();
		
//		googleLookup = new GoogleLocationLookup();
		
		Map<String, PersonData> persons = parser.readPersons(basedir + "Zielpersonen.dat");
		parser.readTrips(basedir + "Wegeinland.dat", persons);
		parser.readLegs(basedir + "Etappen.dat", persons);
		
		Distribution distances = new Distribution();
		Distribution durations = new Distribution();
		Distribution accompanists = new Distribution();
		Distribution leisureTypes = new Distribution();
		Distribution tripTypes = new Distribution();
		
		Distribution referenceDays = new Distribution();
		Distribution ages = new Distribution();
		Distribution numTrips = new Distribution();
		
		Distribution startTimes = new Distribution();
		Distribution endTimes = new Distribution();
		Distribution modes = new Distribution();
		Distribution roundTripModes = new Distribution();
		
		Collection<PersonData> sunday = new LinkedList<PersonData>();
		
		TObjectIntHashMap<String> modeChains = new TObjectIntHashMap<String>();
		for (PersonData pContainer : persons.values()) {
			referenceDays.add(pContainer.referenceDay);
			ages.add(pContainer.age);
			if (pContainer.referenceDay == 7) {
				sunday.add(pContainer);
			}
			
			int nTrips = 0;	
			for (TripData container : pContainer.trips) {
				if(container.roundTrip) {
					roundTripModes.add(container.mode);
					StringBuilder modeChain = new StringBuilder(); 
					for(LegData leg : container.legs) {
						modeChain.append(leg.mode);
						modeChain.append("-");
					}
					modeChains.adjustOrPutValue(modeChain.toString(), 1, 1);
				}
					
				if (pContainer.referenceDay == 7) {
//				if (container.outwardTrip && pContainer.referenceDay == 7) {
					nTrips++;
					distances.add(container.distance);
					durations.add(container.duration);
					accompanists.add(container.accompanists);
					leisureTypes.add(container.leisureType);
					tripTypes.add(container.type);
					startTimes.add(container.startTime);
					endTimes.add(container.endTime);
					modes.add(container.mode);
				}
			}
			numTrips.add(nTrips);
		}
		Distribution.writeHistogram(distances.absoluteDistribution(1.0), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.distance.hist.txt");
		Distribution.writeHistogram(durations.absoluteDistribution(1.0), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.duration.hist.txt");
		Distribution.writeHistogram(accompanists.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.accompanists.hist.txt");
		Distribution.writeHistogram(leisureTypes.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.ltypes.hist.txt");
		Distribution.writeHistogram(referenceDays.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.day.hist.txt");
		Distribution.writeHistogram(ages.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.age.hist.txt");
		Distribution.writeHistogram(numTrips.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.numTrips.hist.txt");
		Distribution.writeHistogram(tripTypes.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.type.hist.txt");
		Distribution.writeHistogram(startTimes.absoluteDistribution(60), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.startTime.hist.txt");
		Distribution.writeHistogram(endTimes.absoluteDistribution(60), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.endTime.hist.txt");
		Distribution.writeHistogram(modes.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.mode.hist.txt");
		Distribution.writeHistogram(roundTripModes.absoluteDistribution(), "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/ltrip.rTripMode.hist.txt");
		
		List<String> chains = ActivityChains.extractTripChains(sunday);
		TObjectIntHashMap<String> chainHist = ActivityChains.makeChainHistogram(chains);
		ActivityChains.writeChainHistogram(chainHist, "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/chains.hist.txt");
		
		ActivityChains.removeDoubleHomes(chains);
		chainHist = ActivityChains.makeChainHistogram(chains);
		ActivityChains.writeChainHistogram(chainHist, "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/chains.filtered.hist.txt");
		
		ActivityChains.writeChainHistogram(modeChains, "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/modechains.hist.txt");
	}

	public Map<String, PersonData> readPersons(String filename) throws IOException {
		Map<String, PersonData> persons = new HashMap<String, PersonData>();
		
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
				
				PersonData container = new PersonData();
				
				String householdId = tokens[colNames.get("HHNR")];
				String personId = tokens[colNames.get("ZIELPNR")]; 
				container.id = householdId + "." + personId;
				container.age = Integer.parseInt(tokens[colNames.get("F23B")]);
				container.referenceDay = Integer.parseInt(tokens[colNames.get("TAG")]);
			
				persons.put(container.id, container);
				
			} catch (Exception e) {
				System.out.println("Failed to parse person: " + e.getMessage());
			}
		}
		logger.info(String.format("Parsed %1$s persons.", persons.size()));
		
		return persons;
	}
	
	public void readTrips(String filename, Map<String, PersonData> persons)
			throws IOException {
//		Map<String, TripContainer> trips = new HashMap<String, TripContainer>();

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		TObjectIntHashMap<String> colNames = readColNames(reader);

		int totalTrips = 0;
		int invalidTrips = 0;
		int outwardTrips = 0;
		
		TripData container = null;
		
		String line = null;
		String[] tokens = null;
		while ((line = reader.readLine()) != null) {
			totalTrips++;
			try {
				tokens = line.split(SEPARATOR);

				container = new TripData();

				String householdId = tokens[colNames.get("HHNR")];
				String personId = tokens[colNames.get("ZIELPNR")]; 
				container.personId = householdId + "." + personId;
				
				container.tripId = tokens[colNames.get("WEGNR")];
				container.distance = Double.parseDouble(tokens[colNames.get("w_dist_subj")]);
				container.startTime = Integer.parseInt(tokens[colNames.get("F58")]);
				container.endTime = Integer.parseInt(tokens[colNames.get("F514")]);
				container.duration = Double.parseDouble(tokens[colNames.get("dauer1")]);
				
				container.mode = Integer.parseInt(tokens[colNames.get("wmittel")]);
				if(container.mode > 7 && container.mode < 14)
					container.aggrMode = AggregatedMode.car;
				else if(container.mode > 1 && container.mode < 8)
					container.aggrMode = AggregatedMode.pt;
				else
					container.aggrMode = AggregatedMode.other;
				
				container.type = Integer.parseInt(tokens[colNames.get("wzweck1")]);
				
				String type2 = tokens[colNames.get("wzweck2")];
				if(type2.equals("1")) {
					outwardTrips++;
					container.outwardTrip = true;
				} else if(type2.equals("3"))
					container.roundTrip = true;
					
				container.accompanists = Integer.parseInt(tokens[colNames.get("F87")]);
				container.accompanists = Math.max(container.accompanists, 0);
				
				container.leisureType = Integer.parseInt(tokens[colNames.get("F5202")]);
				
//				container.startCoord = coordinates(tokens, colNames, "S");
//				container.destCoord = coordinates(tokens, colNames, "Z");
				
				PersonData pContainer = persons.get(container.personId);
				if(pContainer != null) {
					pContainer.trips.add(container);
				} else {
					logger.warn(String.format("Person with id %1$s not found!", container.personId));
				}
				
			} catch (Exception e) {
				invalidTrips++;
			}
		}
		
		logger.info(String.format("Parsed %1$s trips, %2$s outward trips, %3$s invalid trips.", totalTrips, outwardTrips, invalidTrips));
	}

//	private double[] coordinates(String tokens[], TObjectIntHashMap<String> colNames, String prefix) {
//		StringBuilder builder = new StringBuilder();
//		
//		builder.append(tokens[colNames.get(prefix + "_STRA")]);
//		builder.append(" ");
//		builder.append(tokens[colNames.get(prefix + "_HNR")]);
//		builder.append(" ");
//		
//		String plz = tokens[colNames.get(prefix + "_PLZ")];
//		if(!plz.equalsIgnoreCase("-97")) {
//			builder.append(plz);
//			builder.append(" ");
//		}
//		
//		builder.append(tokens[colNames.get(prefix + "_ORT")]);
//		builder.append(" ");
//		builder.append(tokens[colNames.get(prefix + "_LAND")]);
//		builder.append(" ");
//		
//		Coord c = googleLookup.requestCoordinates(builder.toString());
//		return new double[]{c.getX(), c.getY()};
//	}
	
	public void readLegs(String filename, Map<String, PersonData> persons) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		TObjectIntHashMap<String> colNames = readColNames(reader);
		
		int total = 0;
		int invalid = 0;
		
		String line = null;
		String tokens[] = null;
		while((line = reader.readLine()) != null) {
			try {
				tokens = line.split(SEPARATOR);
				
				LegData leg = new LegData();
				leg.id = tokens[colNames.get("ETNR")];
				leg.mode = Integer.parseInt(tokens[colNames.get("F510")]);
				
				String personId = tokens[colNames.get("HHNR")] + "." + tokens[colNames.get("ZIELPNR")];
				PersonData person = persons.get(personId);
				if(person != null) {
					String tripId = tokens[colNames.get("WEGNR")];
					TripData trip = null;
					for(TripData tc : person.trips) {
						if(tc.tripId.equals(tripId)) {
							trip = tc;
							break;
						}
					}
					if(trip != null) {
						trip.legs.add(leg);
					} else {
						invalid++;
//						logger.warn(String.format("Trip with id %1$s for person with id %2$s not found.", tripId, personId));
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
