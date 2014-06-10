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
package playground.agarwalamit.siouxFalls.emissionAnalyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;



/**
 * @author amit after Benjamin
 *
 */
public class EmissionUtilsExtended {
	private final Logger logger = Logger.getLogger(EmissionUtilsExtended.class);
	EmissionUtils emissionUtils = new EmissionUtils(); 

	public  Map<Id, SortedMap<String, Double>> convertPerPersonColdEmissions2String (Population pop, Map<Id, Map<ColdPollutant, Double>> coldEmiss) {
		Map<Id, SortedMap<String, Double>> outColdEmiss = new HashMap<Id, SortedMap<String,Double>>() ;
		for(Person person : pop.getPersons().values()) {
			Id personId = person.getId();
			if (coldEmiss.containsKey(personId)) {
				outColdEmiss.put(personId, emissionUtils.convertColdPollutantMap2String(coldEmiss.get(personId)));
			} else {
				// do nothing
			}
		}
		return outColdEmiss;
	}

	public  Map<Id, SortedMap<String, Double>> convertPerPersonWarmEmissions2String (Population pop, Map<Id, Map<WarmPollutant, Double>> warmEmiss) {
		Map<Id, SortedMap<String, Double>> outWarmEmiss = new HashMap<Id, SortedMap<String,Double>>() ;
		for(Person person : pop.getPersons().values()) {
			Id personId = person.getId();
			if (warmEmiss.containsKey(personId)) {
				outWarmEmiss.put(personId, emissionUtils.convertWarmPollutantMap2String(warmEmiss.get(personId)));
			} else {
				// do nothing
			}
		}
		return outWarmEmiss;
	}

	public Map<String, Double> getTotalColdEmissions(Map<Id, Map<ColdPollutant, Double>> person2TotalColdEmissions) {
		Map<String, Double> totalColdEmissions = new TreeMap<String, Double>();

		for(Id personId : person2TotalColdEmissions.keySet()){
			Map<ColdPollutant, Double> individualColdEmissions = person2TotalColdEmissions.get(personId);
			double sumOfColdPollutant;
			for(ColdPollutant pollutant : individualColdEmissions.keySet()){
				if(totalColdEmissions.containsKey(pollutant.toString())){
					sumOfColdPollutant = totalColdEmissions.get(pollutant.toString()) + individualColdEmissions.get(pollutant);
				} else {
					sumOfColdPollutant = individualColdEmissions.get(pollutant);
				}
				totalColdEmissions.put(pollutant.toString(), sumOfColdPollutant);
			}
		}
		return totalColdEmissions;
	}

	public Map<String, Double> getTotalWarmEmissions(Map<Id, Map<WarmPollutant, Double>> person2TotalWarmEmissions) {
		Map<String, Double> totalWarmEmissions = new TreeMap<String, Double>();

		for(Id personId : person2TotalWarmEmissions.keySet()){
			Map<WarmPollutant, Double> individualWarmEmissions = person2TotalWarmEmissions.get(personId);
			double sumOfWarmPollutant;
			for(WarmPollutant pollutant : individualWarmEmissions.keySet()){
				if(totalWarmEmissions.containsKey(pollutant.toString())){
					sumOfWarmPollutant = totalWarmEmissions.get(pollutant.toString()) + individualWarmEmissions.get(pollutant);
				} else {
					sumOfWarmPollutant = individualWarmEmissions.get(pollutant);
				}
				totalWarmEmissions.put(pollutant.toString(), sumOfWarmPollutant);
			}
		}
		return totalWarmEmissions;
	}

	public  Map<Double, Map<Id, SortedMap<String, Double>>> convertPerLinkColdEmissions2String (Network net, Map<Double,Map<Id, Map<ColdPollutant, Double>>> coldEmiss) {
		Map<Double, Map<Id, SortedMap<String, Double>>> outColdEmiss = new HashMap<Double, Map<Id,SortedMap<String,Double>>>();
		
		for(double t:coldEmiss.keySet()) {
			Map<Id, SortedMap<String, Double>>	 tempMap = new HashMap<Id, SortedMap<String,Double>>();
			for(Id id : coldEmiss.get(t).keySet()){
				tempMap.put(id,	emissionUtils.convertColdPollutantMap2String(coldEmiss.get(t).get(id)));
			}
			outColdEmiss.put(t, emissionUtils.setNonCalculatedEmissionsForNetwork(net, tempMap));
		}
		return outColdEmiss;
	}

	public  Map<Double, Map<Id, SortedMap<String, Double>>> convertPerLinkWarmEmissions2String (Network net, Map<Double,Map<Id, Map<WarmPollutant, Double>>> warmEmiss) {
		Map<Double, Map<Id, SortedMap<String, Double>>> outWarmEmiss = new HashMap<Double, Map<Id,SortedMap<String,Double>>>();
		
		for(double t:warmEmiss.keySet()) {
			Map<Id, SortedMap<String, Double>>	 tempMap = new HashMap<Id, SortedMap<String,Double>>();
			for(Id id : warmEmiss.get(t).keySet()){
				tempMap.put(id,	emissionUtils.convertWarmPollutantMap2String(warmEmiss.get(t).get(id)));
			}
			outWarmEmiss.put(t, emissionUtils.setNonCalculatedEmissionsForNetwork(net, tempMap));
		}
		return outWarmEmiss;
	}
}
