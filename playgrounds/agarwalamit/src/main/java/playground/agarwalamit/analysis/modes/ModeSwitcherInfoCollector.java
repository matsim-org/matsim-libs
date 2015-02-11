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
package playground.agarwalamit.analysis.modes;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

/**
 * A class to store all information of mode switchers
 * @author amit
 */

public class ModeSwitcherInfoCollector {

	private Logger log = Logger.getLogger(ModeSwitcherInfoCollector.class);
	
	public ModeSwitcherInfoCollector(String[] modeSwitchTypes) {
		this.modeSwitchTypes = modeSwitchTypes;
		
		for(String str :modeSwitchTypes){
			this.modeSwitchType2numberOfLegs.put(str, 0);
			this.modeSwitchType2PersonIds.put(str, new ArrayList<Id<Person>>());
			this.modeSwitchType2TripDistances.put(str, new Tuple<Double, Double>(0., 0.));
			this.modeSwitchType2TripTimes.put(str, new Tuple<Double, Double>(0., 0.));
		}
		this.log.info("This will collect the information of mode switchers between two different iterations.");
		this.log.warn("DO NOT USE TRIP TIME AND TRIP DISTANCE MAPS SIMULTANEOUSLY.");
	}

	private String [] modeSwitchTypes;
	
	private SortedMap<String, Tuple<Double, Double>> modeSwitchType2TripDistances = new TreeMap<>();
	private SortedMap<String, Integer> modeSwitchType2numberOfLegs = new TreeMap<>();
	private SortedMap<String, List<Id<Person>>> modeSwitchType2PersonIds = new TreeMap<>();
	private SortedMap<String, Tuple<Double, Double>> modeSwitchType2TripTimes = new TreeMap<>();
	
	public void storeTripTimeInfo(Id<Person> personId, String modeSwitchTyp, Tuple<Double, Double> travelTimes){

		this.modeSwitchType2numberOfLegs.put(modeSwitchTyp, this.modeSwitchType2numberOfLegs.get(modeSwitchTyp)+1);

		List<Id<Person>> swicherIds = this.modeSwitchType2PersonIds.get(modeSwitchTyp);
		swicherIds.add(personId);
		this.modeSwitchType2PersonIds.put(modeSwitchTyp, swicherIds);

		Tuple<Double, Double> now_first_last_its_tripTimes = new Tuple<Double, Double>(this.modeSwitchType2TripTimes.get(modeSwitchTyp).getFirst()+travelTimes.getFirst(), 
				this.modeSwitchType2TripTimes.get(modeSwitchTyp).getSecond()+travelTimes.getSecond());
		this.modeSwitchType2TripTimes.put(modeSwitchTyp, now_first_last_its_tripTimes);
	}
	
	public void storeTripDistanceInfo(Id<Person> personId, String modeSwitchTyp, Tuple<Double, Double> travelDistances){

		this.modeSwitchType2numberOfLegs.put(modeSwitchTyp, this.modeSwitchType2numberOfLegs.get(modeSwitchTyp)+1);

		List<Id<Person>> swicherIds = this.modeSwitchType2PersonIds.get(modeSwitchTyp);
		swicherIds.add(personId);
		this.modeSwitchType2PersonIds.put(modeSwitchTyp, swicherIds);

		Tuple<Double, Double> now_first_last_its_tripTimes = new Tuple<Double, Double>(this.modeSwitchType2TripDistances.get(modeSwitchTyp).getFirst()+travelDistances.getFirst(), 
				this.modeSwitchType2TripDistances.get(modeSwitchTyp).getSecond()+travelDistances.getSecond());
		this.modeSwitchType2TripDistances.put(modeSwitchTyp, now_first_last_its_tripTimes);
	}
	
	public void writeModeSwitcherTripTimes(String runCase){
		String outFile = runCase+"/analysis/modeSwitchersTripTimes.txt";
		BufferedWriter writer =  IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("switchType \t numberOfLegs \t totalTripTimesForFirstIterationInHr \t totalTripTimesForLastIterationInHr \n");

			for(String str:this.modeSwitchTypes){
				writer.write(str+"\t"+this.modeSwitchType2numberOfLegs.get(str)+"\t"
						+this.modeSwitchType2TripTimes.get(str).getFirst()/3600.+"\t"
						+this.modeSwitchType2TripTimes.get(str).getSecond()/3600.+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Data is not written in file. Reason: " + e);
		}
		this.log.info("Data is written to "+outFile);
	}
	
	public void writeModeSwitcherTripDistances(String runCase){
		String outFile = runCase+"/analysis/modeSwitchersTripDistances.txt";
		BufferedWriter writer =  IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("switchType \t numberOfLegs \t totalTripDistancesForFirstIterationInKm \t totalTripDistancesForLastIterationInKm \n");

			for(String str:this.modeSwitchTypes){
				writer.write(str+"\t"+this.modeSwitchType2numberOfLegs.get(str)+"\t"
						+this.modeSwitchType2TripDistances.get(str).getFirst()/1000.+"\t"
						+this.modeSwitchType2TripDistances.get(str).getSecond()/1000.+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Data is not written in file. Reason: " + e);
		}
		this.log.info("Data is written to "+outFile);
	}
	
	public String[] getModeSwitchTypes() {
		return modeSwitchTypes;
	}
	
	public void setModeSwitchTypes(String[] modeSwitchTypes) {
		this.modeSwitchTypes = modeSwitchTypes;
	}
	
	public SortedMap<String, Tuple<Double, Double>> getModeSwitchType2TripDistances() {
		return modeSwitchType2TripDistances;
	}
	
	public SortedMap<String, Integer> getModeSwitchType2numberOfLegs() {
		return modeSwitchType2numberOfLegs;
	}
	
	public SortedMap<String, List<Id<Person>>> getModeSwitchType2PersonIds() {
		return modeSwitchType2PersonIds;
	}
	
	public SortedMap<String, Tuple<Double, Double>> getModeSwitchType2TripTimes() {
		return modeSwitchType2TripTimes;
	}
}
