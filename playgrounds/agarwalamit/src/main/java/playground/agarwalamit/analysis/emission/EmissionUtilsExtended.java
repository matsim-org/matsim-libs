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
import java.util.stream.Collectors;

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

	public  <T> Map<Id<T>, SortedMap<String, Double>> convertPerPersonColdEmissions2String (final Map<Id<T>, Map<ColdPollutant, Double>> coldEmiss) {
		return coldEmiss.entrySet().stream().collect(Collectors.toMap(
			e -> e.getKey(), e->convertColdPollutantMap2String(e.getValue())
		));
	}

	public  <T> Map<Id<T>, SortedMap<String, Double>> convertPerPersonWarmEmissions2String (final Map<Id<T>, Map<WarmPollutant, Double>> warmEmiss) {
		return warmEmiss.entrySet().stream().collect(Collectors.toMap(
			e -> e.getKey(), e -> convertWarmPollutantMap2String(e.getValue())
		));
	}

	public Map<String, Double> getTotalColdEmissions(final Map<Id<Person>, Map<ColdPollutant, Double>> person2TotalColdEmissions) {
		Map<String, Double> totalColdEmissions = new TreeMap<>();

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

	public Map<String, Double> getTotalWarmEmissions(final Map<Id<Person>, Map<WarmPollutant, Double>> person2TotalWarmEmissions) {
		Map<String, Double> totalWarmEmissions = new TreeMap<>();

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

	public  Map<Double, Map<Id<Link>, SortedMap<String, Double>>> convertPerLinkColdEmissions2String (final Network net, final Map<Double,Map<Id<Link>, Map<ColdPollutant, Double>>> coldEmiss) {
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> outColdEmiss = new HashMap<>();
		for(double t:coldEmiss.keySet()) {
			Map<Id<Link>, SortedMap<String, Double>> tempMap = coldEmiss.get(t).entrySet().stream().collect(Collectors
									.toMap(e -> e.getKey(), e -> convertColdPollutantMap2String(e.getValue())));
			outColdEmiss.put(t,setNonCalculatedEmissionsForNetwork(net, tempMap));
		}
		return outColdEmiss;
	}

	public  Map<Double, Map<Id<Link>, SortedMap<String, Double>>> convertPerLinkWarmEmissions2String (final Network net, final Map<Double,Map<Id<Link>, Map<WarmPollutant, Double>>> warmEmiss) {
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> outWarmEmiss = new HashMap<>();
		for(double t:warmEmiss.keySet()) {
			Map<Id<Link>, SortedMap<String, Double>> tempMap = warmEmiss.get(t).entrySet().stream().collect(Collectors.toMap(
					e -> e.getKey(), e -> convertWarmPollutantMap2String(e.getValue())
			));
			outWarmEmiss.put(t, setNonCalculatedEmissionsForNetwork(net, tempMap));
		}
		return outWarmEmiss;
	}

	public Map<WarmPollutant, Double> addTwoWarmEmissionsMap (final Map<WarmPollutant, Double> warmEmission1, final Map<WarmPollutant, Double> warmEmission2){
		return warmEmission1.entrySet().stream().collect(Collectors.toMap(
				e -> e.getKey(), e -> e.getValue() + warmEmission2.get(e.getKey())
		));
	}

	public Map<WarmPollutant, Double> subtractTwoWarmEmissionsMap (final Map<WarmPollutant, Double> warmEmissionBigger, final Map<WarmPollutant, Double> warmEmissionSmaller){
		return warmEmissionBigger.entrySet().stream().collect(Collectors.toMap(
				e -> e.getKey(), e -> e.getValue() - warmEmissionSmaller.get(e.getKey())
		));
	}
}