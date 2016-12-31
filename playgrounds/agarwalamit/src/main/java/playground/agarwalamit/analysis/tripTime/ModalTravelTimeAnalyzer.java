/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.tripTime;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.ListUtils;
import playground.agarwalamit.utils.PersonFilter;

/**
 * @author amit
 */

public class ModalTravelTimeAnalyzer {
	private final SortedMap<String, Double> mode2AvgTripTime = new TreeMap<>();
	private final SortedMap<String, Integer> mode2NumberOfLegs = new TreeMap<>();
	private final SortedMap<String, Double> mode2TotalTravelTime = new TreeMap<>();
	
	private final SortedMap<String, Map<Id<Person>, Double>> mode2PersonId2TotalTravelTime = new TreeMap<>();
	private final SortedMap<String, Map<Id<Person>, Double>> mode2PersonId2AvgTravelTime = new TreeMap<>();
	
	private final FilteredModalTripTravelTimeHandler travelTimeHandler ;
	private final String eventsFile;
	
	public ModalTravelTimeAnalyzer(final String inputEventsFile) {
		this(inputEventsFile,null,null);
	}
	
	public ModalTravelTimeAnalyzer(final String inputEventsFile, final String userGroup, final PersonFilter pf) {
		this.eventsFile = inputEventsFile;
		travelTimeHandler = new FilteredModalTripTravelTimeHandler(userGroup, pf);
	}
	
	public static void main(String[] args) {
		int ITERATION_NR = 100;
		String dir = "../../../repos/runs-svn/patnaIndia/run105/1pct/evac_passing/";
		String eventFile = dir+"/ITERS/it."+ITERATION_NR+"/"+ITERATION_NR+".events.xml.gz";
		String outputFolder = dir+"/analysis/";
		ModalTravelTimeAnalyzer timeAnalyzer  = new ModalTravelTimeAnalyzer(eventFile);
		timeAnalyzer.run();
		timeAnalyzer.writeResults(outputFolder+"/modalTravelTime_it."+ITERATION_NR+".txt");
	}
	
	public void run() {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		events.addHandler(travelTimeHandler);
		reader.readFile(eventsFile);
		
		storeModeData();
		storePersonData();
	}
	
	public void writeResults(String outputFile) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("mode \t totalTime(hr) \t numberOfLegs \t avgTripTime(min) \n");

			for(String mode:mode2AvgTripTime.keySet()){
				writer.write(mode+"\t"+mode2TotalTravelTime.get(mode)/3600 +"\t"+ 
						mode2NumberOfLegs.get(mode)+ "\t" + mode2AvgTripTime.get(mode)/60+ "\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}
	
	private void storeModeData() {
		SortedMap<String, Map<Id<Person>, List<Double>>> times = travelTimeHandler.getLegMode2PesonId2TripTimes();
		for(String mode :times.keySet()){
			double tripTimes =0;
			int count = 0;
			for(Id<Person> id : times.get(mode).keySet()){
				tripTimes += ListUtils.doubleSum(times.get(mode).get(id));
				count += times.get(mode).get(id).size();
			}
			mode2TotalTravelTime.put(mode, tripTimes);
			mode2NumberOfLegs.put(mode, count);
			mode2AvgTripTime.put(mode, tripTimes/count);
		}
	}
	
	private void storePersonData(){
		SortedMap<String, Map<Id<Person>, List<Double>>> times = travelTimeHandler.getLegMode2PesonId2TripTimes();
		
		for(String mode: times.keySet()){
			Map<Id<Person>, Double> personId2TotalTravelTime = new HashMap<>();
			Map<Id<Person>, Double> personId2AvgTravelTime = new HashMap<>();
			
			for(Id<Person> id: times.get(mode).keySet()){
				double totalTravelTime = ListUtils.doubleSum(times.get(mode).get(id));
				double meanTravelTime = ListUtils.doubleMean(times.get(mode).get(id));
				
				personId2TotalTravelTime.put(id, totalTravelTime);
				personId2AvgTravelTime.put(id, meanTravelTime);
			}
			mode2PersonId2TotalTravelTime.put(mode, personId2TotalTravelTime);
			mode2PersonId2AvgTravelTime.put(mode, personId2AvgTravelTime);
		}
	}
	
	/**
	 * @return  Total travel time (summed for all trips for that person) for each person segregated w.r.t. travel modes.
	 */
	public SortedMap<String, Map<Id<Person>, Double>> getMode2PersonId2TotalTravelTime(){
		return this.mode2PersonId2TotalTravelTime;
	}
	
	public SortedMap<String, Map<Id<Person>, Double>> getMode2PersonId2AverageTravelTime(){
		return this.mode2PersonId2AvgTravelTime;
	}
	
	/**
	 * @return  trip time for each trip of each person segregated w.r.t. travel modes.
	 */
	public SortedMap<String, Map<Id<Person>, List<Double>>> getMode2PesonId2TripTimes(){
		return this.travelTimeHandler.getLegMode2PesonId2TripTimes();
	}
	
	public SortedMap<String,Integer> getTravelMode2NumberOfLegs(){
		return this.mode2NumberOfLegs;
	}
	
	public SortedMap<String, Double> getMode2AvgTripTime() {
		return mode2AvgTripTime;
	}

	public SortedMap<String, Double> getMode2TotalTravelTime() {
		return mode2TotalTravelTime;
	}
}