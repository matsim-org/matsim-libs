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
package playground.agarwalamit.munich.analysis.modeSwitcher;

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

	private static final Logger LOG = Logger.getLogger(ModeSwitcherInfoCollector.class);
	private final SortedMap<ModeSwitcherType, Tuple<Double, Double>> modeSwitchType2TripDistances = new TreeMap<>();
	private final SortedMap<ModeSwitcherType, Integer> modeSwitchType2numberOfLegs = new TreeMap<>();
	private final SortedMap<ModeSwitcherType, List<Id<Person>>> modeSwitchType2PersonIds = new TreeMap<>();
	private final SortedMap<ModeSwitcherType, Tuple<Double, Double>> modeSwitchType2TripTimes = new TreeMap<>();

	public ModeSwitcherInfoCollector() {

		for(ModeSwitcherType str :ModeSwitcherType.values()){
			this.modeSwitchType2numberOfLegs.put(str, 0);
			this.modeSwitchType2PersonIds.put(str, new ArrayList<>());
			this.modeSwitchType2TripDistances.put(str, new Tuple<>(0., 0.));
			this.modeSwitchType2TripTimes.put(str, new Tuple<>(0., 0.));
		}
		LOG.info("This will collect the information of mode switchers between two different iterations.");
		LOG.warn("DO NOT USE TRIP TIME AND TRIP DISTANCE MAPS SIMULTANEOUSLY.");
	}

	public void storeTripTimeInfo(final Id<Person> personId, final ModeSwitcherType modeSwitchTyp, final Tuple<Double, Double> travelTimes){

		this.modeSwitchType2numberOfLegs.put(modeSwitchTyp, this.modeSwitchType2numberOfLegs.get(modeSwitchTyp)+1);

		List<Id<Person>> swicherIds = this.modeSwitchType2PersonIds.get(modeSwitchTyp);
		swicherIds.add(personId);
		this.modeSwitchType2PersonIds.put(modeSwitchTyp, swicherIds);

		Tuple<Double, Double> nowFirstLastItsTripTimes = new Tuple<>(this.modeSwitchType2TripTimes.get(modeSwitchTyp).getFirst() + travelTimes.getFirst(),
                this.modeSwitchType2TripTimes.get(modeSwitchTyp).getSecond() + travelTimes.getSecond());
		this.modeSwitchType2TripTimes.put(modeSwitchTyp, nowFirstLastItsTripTimes);
	}

	public void storeTripDistanceInfo(final Id<Person> personId, final ModeSwitcherType modeSwitchTyp, final Tuple<Double, Double> travelDistances){

		this.modeSwitchType2numberOfLegs.put(modeSwitchTyp, this.modeSwitchType2numberOfLegs.get(modeSwitchTyp)+1);

		List<Id<Person>> swicherIds = this.modeSwitchType2PersonIds.get(modeSwitchTyp);
		swicherIds.add(personId);
		this.modeSwitchType2PersonIds.put(modeSwitchTyp, swicherIds);

		Tuple<Double, Double> nowFirstLastItsTripTimes = new Tuple<>(this.modeSwitchType2TripDistances.get(modeSwitchTyp).getFirst() + travelDistances.getFirst(),
                this.modeSwitchType2TripDistances.get(modeSwitchTyp).getSecond() + travelDistances.getSecond());
		this.modeSwitchType2TripDistances.put(modeSwitchTyp, nowFirstLastItsTripTimes);
	}

	public void writeModeSwitcherTripTimes(final String runCase){
		String outFile = runCase+"/analysis/modeSwitchersTripTimes.txt";
		BufferedWriter writer =  IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("switchType \t numberOfLegs \t totalTripTimesForFirstIterationInHr \t totalTripTimesForLastIterationInHr \n");

			for(ModeSwitcherType str:this.modeSwitchType2numberOfLegs.keySet()){
				writer.write(str+"\t"+this.modeSwitchType2numberOfLegs.get(str)+"\t"
						+this.modeSwitchType2TripTimes.get(str).getFirst()/3600.+"\t"
						+this.modeSwitchType2TripTimes.get(str).getSecond()/3600.+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Data is not written in file. Reason: " + e);
		}
		LOG.info("Data is written to "+outFile);
	}

	public void writeModeSwitcherTripDistances(final String runCase){
		String outFile = runCase+"/analysis/modeSwitchersTripDistances.txt";
		BufferedWriter writer =  IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("switchType \t numberOfLegs \t totalTripDistancesForFirstIterationInKm \t totalTripDistancesForLastIterationInKm \n");

			for(ModeSwitcherType str: this.modeSwitchType2numberOfLegs.keySet()){
				writer.write(str+"\t"+this.modeSwitchType2numberOfLegs.get(str)+"\t"
						+this.modeSwitchType2TripDistances.get(str).getFirst()/1000.+"\t"
						+this.modeSwitchType2TripDistances.get(str).getSecond()/1000.+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Data is not written in file. Reason: " + e);
		}
		LOG.info("Data is written to "+outFile);
	}

	public SortedMap<ModeSwitcherType, Tuple<Double, Double>> getModeSwitchType2TripDistances() {
		return modeSwitchType2TripDistances;
	}

	public SortedMap<ModeSwitcherType, Integer> getModeSwitchType2numberOfLegs() {
		return modeSwitchType2numberOfLegs;
	}

	public SortedMap<ModeSwitcherType, List<Id<Person>>> getModeSwitchType2PersonIds() {
		return modeSwitchType2PersonIds;
	}

	public SortedMap<ModeSwitcherType, Tuple<Double, Double>> getModeSwitchType2TripTimes() {
		return modeSwitchType2TripTimes;
	}
}
