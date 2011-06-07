/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeRemovedFacilities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.energyflows.facilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;

public class AnalyzeRemovedFacilities {
	
	final private static Logger log = Logger.getLogger(AnalyzeRemovedFacilities.class);
	
	private Set<String> activityOptionTypes;	// set of all found ActivityTypes
	private Set<Id> links;	// all links, that host facilities that are removed
	private Map<Id, List<ActivityFacility>> hostedFacilities;	// linkId, List of Facilities connected to that link
	private Map<String, Double> removedCapacities;	// ActivityType, Capacity
	private Map<String, List<ActivityFacility>> removedActivityTypes;	// ActivityType, List of Facilities with this ActivityType
	
	/*
	 * Analyze the location (assigned link) and capacities of the facilities that are removed.
	 * The facilities that replace them should offer the same capacities. 	
	 */
	public AnalyzeRemovedFacilities(List<ActivityFacility> removedFacilities) {
		
		activityOptionTypes = new TreeSet<String>();
		links = new TreeSet<Id>();	
		hostedFacilities = new TreeMap<Id, List<ActivityFacility>>();	
		removedCapacities = new TreeMap<String, Double>();	
		removedActivityTypes = new TreeMap<String, List<ActivityFacility>>();	
		
		/*
		 * Identify all ActivityOptions
		 */
		for (ActivityFacility facility : removedFacilities) {
			for (ActivityOption activityOption : facility.getActivityOptions().values()) {
				String s = new String(activityOption.getType()).intern();
				activityOptionTypes.add(s);
				
				// add entries in the maps, if they are not already existing
				if (removedCapacities.get(s) == null) removedCapacities.put(s, 0.0);
				if (removedActivityTypes.get(s) == null) removedActivityTypes.put(s, new ArrayList<ActivityFacility>());
			}
			links.add(facility.getLinkId());
			List<ActivityFacility> list = hostedFacilities.get(facility.getLinkId());
			if (list == null) {
				list = new ArrayList<ActivityFacility>();
				hostedFacilities.put(facility.getLinkId(), list);
			}
			list.add(facility);

			getRemovedCapacity(facility);
		}
		log.info("number of links that host removed facilities: " + links.size());
		log.info("identified activity option types:");
		for (String string : activityOptionTypes) {
			log.info(string);
		}
		
		for (String string : removedCapacities.keySet()) {
			log.info("removed " + string + " capacity: " + removedCapacities.get(string));
		}
		for (String string : removedActivityTypes.keySet()) {
			log.info("removed " + string + " activity types: " + removedActivityTypes.get(string).size());
		}
	}
	
	private void getRemovedCapacity(ActivityFacility facility) {
		
		ActivityOption activityOption = null;
		for (String optionString : removedCapacities.keySet()) {
			activityOption = facility.getActivityOptions().get(optionString);
			if (activityOption != null) {
				double rc = removedCapacities.get(optionString);
				removedCapacities.put(optionString, rc + activityOption.getCapacity());
				
				List<ActivityFacility> list = removedActivityTypes.get(optionString);
				list.add(facility);
			}			
		}
	}
	
	public Map<String, Double> getRemovedCapacities() {
		return Collections.unmodifiableMap(this.removedCapacities);
	}
	
	public Map<Id, List<ActivityFacility>> getHostedFacilities() {
		return this.hostedFacilities;
	}
}
