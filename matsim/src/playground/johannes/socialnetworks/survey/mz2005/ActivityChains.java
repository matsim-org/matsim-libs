/* *********************************************************************** *
 * project: org.matsim.*
 * TripChains.java
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
import gnu.trove.TObjectIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author illenberger
 *
 */
public class ActivityChains {

//	public static enum ActivityTypes {home, nonleisure, visit, bar, loutdoor, lindoor};
	
	public static List<String> extractTripChains(Collection<PersonContainer> persons) {
		List<String> chains = new LinkedList<String>();
		
		int invalidChains = 0;
		
		for(PersonContainer person : persons) {
			try {
			/*
			 * Sort the trips to chronological order.
			 */
			SortedSet<TripContainer> trips = new TreeSet<TripContainer>(new Comparator<TripContainer>() {
				public int compare(TripContainer o1, TripContainer o2) {
					
					int result = o1.startTime - o2.endTime;
					if(result == 0) {
						if(o1 == o2)
							return 0;
						else
							return o1.hashCode() - o2.hashCode();
					} else
						return result;
				}
			});
			
			trips.addAll(person.trips);
			
			List<ActivityType> chain = new LinkedList<ActivityType>();
			
			chain.add(ActivityType.home);
			
			for(TripContainer trip : trips) {
				if(trip.outwardTrip) {
					if(trip.type == 8) {
						if(trip.leisureType == 1) {
							chain.add(ActivityType.visit);
						} else if (trip.leisureType == 2) {
							chain.add(ActivityType.bar);
						} else if (trip.leisureType == 3 || trip.leisureType == 9) {
							chain.add(ActivityType.loutdoor);
						} else if (trip.leisureType < 1) {
							throw new IllegalArgumentException("Type not specified! Dropping complete activity chain.");
						} else {
							chain.add(ActivityType.lindoor);
						}
					} else {
						chain.add(ActivityType.nonleisure);
					}
				} else {
					chain.add(ActivityType.home);
				}
			}
			
			StringBuilder builder = new StringBuilder(chain.size() * 10);
			for(ActivityType t : chain) {
				builder.append(t);
				builder.append("-");
			}
			
			chains.add(builder.toString());
			
			} catch (IllegalArgumentException e) {
				invalidChains++;
			}
		}
		System.out.println(String.format("Total chains = %1$s, invalid = %2$s.", chains.size(), invalidChains));
		return chains;
	}
	
	public static TObjectIntHashMap<String> makeChainHistogram(List<String> chains) {
		TObjectIntHashMap<String> hist = new TObjectIntHashMap<String>();
		for(String chain : chains) {
			hist.adjustOrPutValue(chain, 1, 1);
		}
		return hist;
	}
	
	public static List<String> removeDoubleHomes(List<String> chains) {
		for(int i= 0; i < chains.size(); i++) {
			String chain = chains.get(i);
			String newChain = chain.replaceAll("home-home", "home");
			chains.set(i, newChain);
		}
		return chains;
	}
	
	public static void writeChainHistogram(TObjectIntHashMap<String> hist, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("chain\tcount");
		writer.newLine();
		
		TObjectIntIterator<String> it = hist.iterator();
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			writer.write(it.key());
			writer.write("\t");
			writer.write(String.valueOf(it.value()));
			writer.newLine();
		}
		writer.close();
	}
}
