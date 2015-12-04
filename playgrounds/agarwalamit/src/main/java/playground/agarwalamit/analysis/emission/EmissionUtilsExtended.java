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
package playground.agarwalamit.analysis.emission;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;

/**
 * @author amit
 *
 */
public class EmissionUtilsExtended extends EmissionUtils{


	public  Map<Id<Person>, SortedMap<String, Double>> convertPerPersonColdEmissions2String ( Map<Id<Person>, Map<ColdPollutant, Double>> coldEmiss) {
		Map<Id<Person>, SortedMap<String, Double>> outColdEmiss = new HashMap<>() ;
		for(Id<Person> personId : coldEmiss.keySet()) {
			outColdEmiss.put(personId, convertColdPollutantMap2String(coldEmiss.get(personId)));
		}
		return outColdEmiss;
	}

	public  Map<Id<Person>, SortedMap<String, Double>> convertPerPersonWarmEmissions2String (Map<Id<Person>, Map<WarmPollutant, Double>> warmEmiss) {
		Map<Id<Person>, SortedMap<String, Double>> outWarmEmiss = new HashMap<>();
		for(Id<Person> personId : warmEmiss.keySet()) {
			outWarmEmiss.put(personId, convertWarmPollutantMap2String(warmEmiss.get(personId)));
		}
		return outWarmEmiss;
	}

	public Map<String, Double> getTotalColdEmissions(Map<Id<Person>, Map<ColdPollutant, Double>> person2TotalColdEmissions) {
		Map<String, Double> totalColdEmissions = new TreeMap<String, Double>();

		for(Id<Person> personId : person2TotalColdEmissions.keySet()){
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

	public Map<String, Double> getTotalWarmEmissions(Map<Id<Person>, Map<WarmPollutant, Double>> person2TotalWarmEmissions) {
		Map<String, Double> totalWarmEmissions = new TreeMap<String, Double>();

		for(Id<Person> personId : person2TotalWarmEmissions.keySet()){
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

	public  Map<Double, Map<Id<Link>, SortedMap<String, Double>>> convertPerLinkColdEmissions2String (Network net, Map<Double,Map<Id<Link>, Map<ColdPollutant, Double>>> coldEmiss) {
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> outColdEmiss = new HashMap<>();
		for(double t:coldEmiss.keySet()) {
			Map<Id<Link>, SortedMap<String, Double>> tempMap = new HashMap<>();
			for(Id<Link> id : coldEmiss.get(t).keySet()){
				tempMap.put(id,	convertColdPollutantMap2String(coldEmiss.get(t).get(id)));
			}
			outColdEmiss.put(t,setNonCalculatedEmissionsForNetwork(net, tempMap));
		}
		return outColdEmiss;
	}

	public  Map<Double, Map<Id<Link>, SortedMap<String, Double>>> convertPerLinkWarmEmissions2String (Network net, Map<Double,Map<Id<Link>, Map<WarmPollutant, Double>>> warmEmiss) {
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> outWarmEmiss = new HashMap<>();

		for(double t:warmEmiss.keySet()) {
			Map<Id<Link>, SortedMap<String, Double>>	 tempMap = new HashMap<>();
			for(Id<Link> id : warmEmiss.get(t).keySet()){
				tempMap.put(id,	convertWarmPollutantMap2String(warmEmiss.get(t).get(id)));
			}
			outWarmEmiss.put(t, setNonCalculatedEmissionsForNetwork(net, tempMap));
		}
		return outWarmEmiss;
	}
	public Map<WarmPollutant, Double> addTwoWarmEmissionsMap (Map<WarmPollutant, Double> warmEmission1,Map<WarmPollutant, Double> warmEmission2){
		Map<WarmPollutant, Double> warmEmissionOut = new HashMap<WarmPollutant, Double>();

		for(WarmPollutant wm : warmEmission1.keySet()){
			warmEmissionOut.put(wm, warmEmission1.get(wm)+warmEmission2.get(wm));
		}
		return warmEmissionOut;
	}
	public Map<WarmPollutant, Double> subtractTwoWarmEmissionsMap (Map<WarmPollutant, Double> warmEmissionBigger,Map<WarmPollutant, Double> warmEmissionSmaller){
		Map<WarmPollutant, Double> warmEmissionOut = new HashMap<WarmPollutant, Double>();

		for(WarmPollutant wm : warmEmissionBigger.keySet()){
			warmEmissionOut.put(wm, warmEmissionBigger.get(wm)-warmEmissionSmaller.get(wm));
		}
		return warmEmissionOut;
	}
}
