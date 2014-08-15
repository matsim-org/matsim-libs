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
package playground.agarwalamit.analysis.legModeHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

/**
 * @author amit
 */
public class LegModeTravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final Logger logger = Logger.getLogger(LegModeTravelTimeHandler.class);
	private Map<String, Map<Id, Double>> mode2PersonId2TravelTime;

	public LegModeTravelTimeHandler() {
		this.mode2PersonId2TravelTime = new HashMap<String, Map<Id,Double>>();
	}

	@Override
	public void reset(int iteration) {
		this.mode2PersonId2TravelTime.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String legMode = event.getLegMode();
		Id personId = event.getPersonId();
		double arrivalTime =event.getTime();

		if(this.mode2PersonId2TravelTime.containsKey(legMode)){
			Map<Id, Double> personId2TravelTime = this.mode2PersonId2TravelTime.get(legMode);
			if(personId2TravelTime.containsKey(personId)){
				double travelTimeSoFar = personId2TravelTime.get(personId);
				double newTravelTime = travelTimeSoFar+arrivalTime;
				personId2TravelTime.put(personId, newTravelTime);
			} else {
				personId2TravelTime.put(personId, arrivalTime);
			}
		} else {
			Map<Id, Double> personId2TravelTime = new HashMap<Id, Double>();
			personId2TravelTime.put(personId, arrivalTime);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String legMode = event.getLegMode();
		Id personId = event.getPersonId();
		double deartureTime =event.getTime();

		if(this.mode2PersonId2TravelTime.containsKey(legMode)){
			Map<Id, Double> personId2TravelTime = this.mode2PersonId2TravelTime.get(legMode);
			if(personId2TravelTime.containsKey(personId)){
				double travelTimeSoFar = personId2TravelTime.get(personId);
				double newTravelTime = travelTimeSoFar-deartureTime;
				personId2TravelTime.put(personId, newTravelTime);
			} else {
				personId2TravelTime.put(personId, -deartureTime);
			}
		} else {
			Map<Id, Double> personId2TravelTime = new HashMap<Id, Double>();
			personId2TravelTime.put(personId, -deartureTime);
			this.mode2PersonId2TravelTime.put(legMode, personId2TravelTime);
		}
	}

	public Map<String, Map<Id, Double>> getLegMode2PersonId2TravelTime(){
		return this.mode2PersonId2TravelTime;
	}

	public SortedMap<String,Double> getTravelMode2MeanTime(){
		return calculateTravelMode2MeanTravelTime();
	}
	
	public SortedMap<String,Double> getTravelMode2MedianTime(){
		return calculateTravelMode2MedianTravelTime();
	}

	private SortedMap<String, Double> calculateTravelMode2MeanTravelTime(){	
		logger.info("Calculating mean(average) travel time for all travel modes.");
		SortedMap<String, Double> mode2Mean = new TreeMap<String, Double>();
		List<Double> allTravelTimes = new ArrayList<Double>();
		for(String mode : mode2PersonId2TravelTime.keySet()){
			List<Double> travelTimes = new ArrayList<Double>();
			travelTimes.addAll(mode2PersonId2TravelTime.get(mode).values());
			allTravelTimes.addAll(mode2PersonId2TravelTime.get(mode).values());
			mode2Mean.put(mode, calculateMean(travelTimes));
		}
		mode2Mean.put("allModes", calculateMean(allTravelTimes));
		return mode2Mean;
	}
	
	private SortedMap<String, Double> calculateTravelMode2MedianTravelTime(){
		logger.info("Calculating median travel time for all travel modes.");
		SortedMap<String, Double> mode2Median = new TreeMap<String, Double>();
		List<Double> allTravelTimes = new ArrayList<Double>();
		for(String mode : mode2PersonId2TravelTime.keySet()){
			List<Double> travelTimes = new ArrayList<Double>();
			travelTimes.addAll(mode2PersonId2TravelTime.get(mode).values());
			allTravelTimes.addAll(mode2PersonId2TravelTime.get(mode).values());
			mode2Median.put(mode, calculateMedian(travelTimes));
		}
		mode2Median.put("allModes", calculateMedian(allTravelTimes));
		return mode2Median;
	}
	
	private double calculateMedian(List<Double> inputList){
		Collections.sort(inputList);
		int middle = inputList.size()/2;
	    if (inputList.size()%2 == 1) {
	        return inputList.get(middle);
	    } else {
	        return (inputList.get(middle-1) + inputList.get(middle)) / 2.0;
	    }
	}
	
	private double calculateMean(List<Double> inputList){
		double sum = 0;
		for(double d:inputList){
			sum +=d;
		}
		return sum/inputList.size();
	}
}
