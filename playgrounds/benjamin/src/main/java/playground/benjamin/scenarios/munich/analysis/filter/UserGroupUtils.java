/* *********************************************************************** *
 * project: org.matsim.*
 * UserGroupUtils.java
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
package playground.benjamin.scenarios.munich.analysis.filter;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

/**
 * @author benjamin
 *
 */
public class UserGroupUtils {
	
	PersonFilter personFilter = new PersonFilter();

	public UserGroupUtils() {
		this.personFilter = new PersonFilter();
	}
	
	public SortedMap<UserGroup, SortedMap<String, Double>> getEmissionsPerGroup(Map<Id, SortedMap<String, Double>> person2TotalEmissions) {
		SortedMap<UserGroup, SortedMap<String, Double>> userGroup2Emissions = new TreeMap<UserGroup, SortedMap<String, Double>>();
		
		for(UserGroup userGroup : UserGroup.values()){
			SortedMap<String, Double> totalEmissions = new TreeMap<String,Double>();
			for(Id personId : person2TotalEmissions.keySet()){
				SortedMap<String, Double> individualEmissions = person2TotalEmissions.get(personId);
				if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
					double sumOfPollutant;
					for(String pollutant : individualEmissions.keySet()){
						if(totalEmissions.containsKey(pollutant)){
							sumOfPollutant = totalEmissions.get(pollutant) + individualEmissions.get(pollutant);
						} else {
							sumOfPollutant = individualEmissions.get(pollutant);
						}
						totalEmissions.put(pollutant, sumOfPollutant);
					}
				}
			}
			userGroup2Emissions.put(userGroup, totalEmissions);
		}
		return userGroup2Emissions;
	}

}
