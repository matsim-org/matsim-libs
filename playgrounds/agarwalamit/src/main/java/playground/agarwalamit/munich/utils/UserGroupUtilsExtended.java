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
package playground.agarwalamit.munich.utils;

import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit
 */
public class UserGroupUtilsExtended {
	private final Logger logger = Logger.getLogger(UserGroupUtilsExtended.class);

	public SortedMap<String, Double> calculateTravelMode2Mean(Map<String, Map<Id<Person>, Double>> inputMap, Population relavantPop){	
		this.logger.info("Calculating mean(average) ...");
		SortedMap<String, Double> mode2Mean = new TreeMap<>();
		List<Double> allQty = new ArrayList<>();
		for(String mode : inputMap.keySet()){
			List<Double> quants = new ArrayList<>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(relavantPop.getPersons().keySet().contains(id)){
					quants.add(inputMap.get(mode).get(id));
					allQty.add(inputMap.get(mode).get(id));
				}
			}
			mode2Mean.put(mode, ListUtils.doubleMean(quants));
		}
		mode2Mean.put("allModes", ListUtils.doubleMean(allQty));
		return mode2Mean;
	}

	public SortedMap<String, Double> calculateTravelMode2MeanFromLists(Map<String, Map<Id<Person>, List<Double>>> inputMap, Population relavantPop){	
		this.logger.info("Calculating mean(average) ...");
		SortedMap<String, Double> mode2Mean = new TreeMap<>();
		List<Double> allQty = new ArrayList<>();
		for(String mode : inputMap.keySet()){
			List<Double> quants = new ArrayList<>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(relavantPop.getPersons().keySet().contains(id)){
					quants.addAll(inputMap.get(mode).get(id));
					allQty.addAll(inputMap.get(mode).get(id));
				}
			}
			mode2Mean.put(mode, ListUtils.doubleMean(quants));
		}
		mode2Mean.put("allModes", ListUtils.doubleMean(allQty));
		return mode2Mean;
	}

	public SortedMap<String, Double> calculateTravelMode2Median(Map<String, Map<Id<Person>, Double>> inputMap, Population relavantPop){
		this.logger.info("Calculating median ...");
		SortedMap<String, Double> mode2Median = new TreeMap<>();
		List<Double> allQty = new ArrayList<>();
		for(String mode : inputMap.keySet()){
			List<Double> quants = new ArrayList<>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(relavantPop.getPersons().keySet().contains(id)){
					quants.add(inputMap.get(mode).get(id));
					allQty.add(inputMap.get(mode).get(id));
				}
			}
			mode2Median.put(mode, calculateMedian(quants));
		}
		mode2Median.put("allModes", calculateMedian(allQty));
		return mode2Median;
	}

	public SortedMap<String, Double> calculateTravelMode2MedianFromLists(Map<String, Map<Id<Person>, List<Double>>> inputMap, Population relavantPop){
		this.logger.info("Calculating median ...");
		SortedMap<String, Double> mode2Median = new TreeMap<>();
		List<Double> allQty = new ArrayList<>();
		for(String mode : inputMap.keySet()){
			List<Double> quants = new ArrayList<>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(relavantPop.getPersons().keySet().contains(id)){
					quants.addAll(inputMap.get(mode).get(id));
					allQty.addAll(inputMap.get(mode).get(id));
				}
			}
			mode2Median.put(mode, calculateMedian(quants));
		}
		mode2Median.put("allModes", calculateMedian(allQty));
		return mode2Median;
	}

	public List<Double> getTotalStatListForBoxPlot(Map<Id<Person>, Double> inputMap, Population relevantPop){
		List<Double> data = new ArrayList<>();
		for(Id<Person> id :inputMap.keySet()){
			if(relevantPop.getPersons().keySet().contains(id)){
				data.add(inputMap.get(id));
			}
		}
		return data;
	}

	public double calculateMedian(List<Double> inputList){
		if(inputList.isEmpty()){
			return 0.;
		} else {
			Collections.sort(inputList);
			int middle = inputList.size()/2;
			if (inputList.size()%2 == 1) {
				return inputList.get(middle);
			} else {
				return (inputList.get(middle-1) + inputList.get(middle)) / 2.0;
			}
		}
	}
}
