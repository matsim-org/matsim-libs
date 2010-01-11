/* *********************************************************************** *
 * project: org.matsim.*
 * Analyzer.java
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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.contrib.sna.math.Distribution;


/**
 * @author illenberger
 *
 */
public class Analyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String basedir = "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/mz2005/rawdata/";
		String outbase = "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/mz2005/analysis/";
		TripParser parser = new TripParser();
		System.out.println("Parsing persons...");
		Map<String, PersonContainer> persons = parser.readPersons(basedir + "Zielpersonen.dat");
		System.out.println("Parsing trips...");
		parser.readTrips(basedir + "Wegeinland.dat", persons);
		System.out.println("Parsing legs...");
		parser.readLegs(basedir + "Etappen.dat", persons);

		System.out.println("Creating pseudo plans...");
		for(PersonContainer person : persons.values()) {
			person.createPseudoPlan();
			person.removeDoubleHomes();
		}
		
		for(int day = 1; day < 8; day++) {
			System.out.println("Analysing day " + day + "...");
			String output = outbase + "day" + day + "/";
			new File(output).mkdirs();
			
			Collection<PersonContainer> daypersons = new LinkedList<PersonContainer>();
			for(PersonContainer person : persons.values()) {
				if(person.referenceDay == day) {
					daypersons.add(person);
				}
			}
			analysis(daypersons, output);
		}
		
		System.out.println("Analysing all days...");
		new File(outbase + "all/").mkdirs();
		analysis(persons.values(), outbase + "all/");
	}

	public static void analysis(Collection<PersonContainer> persons, String output) throws IOException {
		/*
		 * Activity chains
		 */
		TObjectIntHashMap<String> hist = PseudoPlanAnalyzer.makeChainHistogram(persons);
		PseudoPlanAnalyzer.writeChainHistogram(hist, output + "actchain.hist.txt");
		/*
		 * Activity type frequencies
		 */
		Distribution acttypes = new Distribution();
		for(PersonContainer person : persons) {
			for(PseudoActivity act : person.plan.activities) {
				acttypes.add(act.type.ordinal());
			}
		}
		Distribution.writeHistogram(acttypes.absoluteDistribution(), output + "acttype.hist.txt");
		/*
		 * Activity frequencies per type
		 */
		Distribution[] actfrequencies = new Distribution[ActivityType.values().length];
		for(int i = 0; i < actfrequencies.length; i++)
			actfrequencies[i] = new Distribution();
		Distribution leisurefrequencies = new Distribution();
		
		for(PersonContainer person : persons) {
			int[] counts = new int[ActivityType.values().length];
			int leisurecounts = 0;
			
			for(PseudoActivity act : person.plan.activities) {
				counts[act.type.ordinal()]++;
				
				if(act.type != ActivityType.nonleisure && act.type != ActivityType.home)
					leisurecounts++;
			}
			for(int i = 0; i < actfrequencies.length; i++)
				actfrequencies[i].add(counts[i]);
			
			leisurefrequencies.add(leisurecounts);
		}
		for(int i = 0; i < actfrequencies.length; i++)
			Distribution.writeHistogram(actfrequencies[i].absoluteDistribution(), output + ActivityType.values()[i].name() + ".hist.txt");
		
		Distribution.writeHistogram(leisurefrequencies.absoluteDistribution(), output + "leisure.hist.txt");
		/*
		 * Activity durations
		 */
		new File(output + "all/").mkdirs();
		analyseActivityTiming(persons, output + "all/", null);
		for(AggregatedMode mode : AggregatedMode.values()) {
			new File(output + mode.toString() + "/").mkdirs();
			analyseActivityTiming(persons, output + mode.toString() + "/", mode);
		}
		/*
		 * Activity load
		 */
		loadCurve(persons, output);
	}
	
	public static void analyseActivityTiming(Collection<PersonContainer> persons, String output, AggregatedMode mode) throws FileNotFoundException, IOException {
		Distribution[] actdurations = new Distribution[ActivityType.values().length];
		Distribution[] actstarttimes = new Distribution[ActivityType.values().length];
		Distribution[] distances = new Distribution[ActivityType.values().length];
		for(int i = 0; i < actdurations.length; i++) {
			actdurations[i] = new Distribution();
			actstarttimes[i] = new Distribution();
			distances[i] = new Distribution();
		}
		int[] zerodurs = new int[ActivityType.values().length];
		for(PersonContainer person : persons) {
			for(int i = 0; i < person.plan.activities.size(); i++) {
				PseudoActivity act = person.plan.activities.get(i);
				if(mode == null) {
					int dur = act.endTime - act.startTime;
					if(dur == 0)
						zerodurs[act.type.ordinal()]++;
					else
						actdurations[act.type.ordinal()].add(dur);
					actstarttimes[act.type.ordinal()].add(act.startTime);
					
					if(i > 0 && person.plan.trips.get(i-1).outwardTrip) {
						double dist = person.plan.trips.get(i-1).distance;
						if(dist > 0)
							distances[act.type.ordinal()].add(dist);
					}
				} else {
					if(i > 0) {
						if(person.plan.trips.get(i-1).outwardTrip && person.plan.trips.get(i-1).aggrMode == mode) {
							int dur = act.endTime - act.startTime;
							if(dur == 0)
								zerodurs[act.type.ordinal()]++;
							else
								actdurations[act.type.ordinal()].add(dur);
							actstarttimes[act.type.ordinal()].add(act.startTime);
							
							double dist = person.plan.trips.get(i-1).distance;
							if(dist > 0)
								distances[act.type.ordinal()].add(dist);
						}
					}
				}
			}
		}
		for(int i = 0; i < ActivityType.values().length; i++)
			System.out.println(String.format("%1$s activities of type %2$s have duration=0.", zerodurs[i], ActivityType.values()[i].toString()));
		for(int i = 0; i < actdurations.length; i++) {
			Distribution.writeHistogram(actdurations[i].absoluteDistribution(10), output + ActivityType.values()[i].name() + ".dur.hist.txt");
			Distribution.writeHistogram(actstarttimes[i].absoluteDistribution(10), output + ActivityType.values()[i].name() + ".start.hist.txt");
			Distribution.writeHistogram(distances[i].absoluteDistribution(1), output + ActivityType.values()[i].name() + ".distance.hist.txt");
		}
	}
	
	public static void loadCurve(Collection<PersonContainer> persons, String output) throws FileNotFoundException, IOException {
		TDoubleDoubleHashMap[] loadCurves = new TDoubleDoubleHashMap[ActivityType.values().length];
		for(int i = 0; i < loadCurves.length; i++)
			loadCurves[i] = new TDoubleDoubleHashMap();
		
		for(PersonContainer person : persons) {
			for(PseudoActivity act : person.plan.activities) {
				TDoubleDoubleHashMap loadCurve = loadCurves[act.type.ordinal()];
				for(int t = act.startTime; t < act.endTime; t++) {
					loadCurve.adjustOrPutValue(t, 1, 1);
				}
			}
		}
		
		for(ActivityType type : ActivityType.values()) {
			Distribution.writeHistogram(loadCurves[type.ordinal()], output + type.name() + ".load.hist.txt");
		}
	}
}
