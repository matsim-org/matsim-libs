/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoicePlanAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.telaviv.facilities.FacilitiesCreator;
import playground.telaviv.zones.ZoneMapping;

/*
 * Use fixed probabilities over the day which are read from a text file.
 */
public class LocationChoicePlanAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(LocationChoicePlanAlgorithm.class);
	
	private Scenario scenario;
	private LocationChoiceProbabilityCreator locationChoiceProbabilityCreator = null;
	private ZoneMapping zoneMapping = null;
	private Map<Id, List<Integer>> shoppingActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	private Map<Integer, List<ActivityFacility>> facilityLocationMap;
	
	private Random random = MatsimRandom.getLocalInstance();
	
	LocationChoicePlanAlgorithm(Scenario scenario,
			LocationChoiceProbabilityCreator locationChoiceProbabilityCreator,
			ZoneMapping zoneMapping, Map<Id, List<Integer>> shoppingActivities) {
		this.scenario = scenario;
		this.locationChoiceProbabilityCreator = locationChoiceProbabilityCreator;
		this.zoneMapping = zoneMapping;
		this.shoppingActivities = shoppingActivities;
		
		ObjectAttributes objectAttributes = scenario.getActivityFacilities().getFacilityAttributes();
		
		facilityLocationMap = new HashMap<Integer, List<ActivityFacility>>();
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			Object tazObject = objectAttributes.getAttribute(facility.getId().toString(), FacilitiesCreator.TAZObjectAttributesName);
			if (tazObject != null) {
				int taz = (Integer) tazObject;
				List<ActivityFacility> list = facilityLocationMap.get(taz);
				if (list == null) {
					list = new ArrayList<ActivityFacility>();
					facilityLocationMap.put(taz, list);
				}
				list.add(facility);
			}
		}
	}
	
	/*
	 * Searches for shopping activities that can be replaced.
	 * If no ones are found no replanning is necessary and therefore
	 * false is returned.
	 */
	@Override
	public void run(Plan plan) {
		List<Integer> shoppingIndices = shoppingActivities.get(plan.getPerson().getId());
		
		List<PlanElement> planElements = plan.getPlanElements();

		// if List is null, the Person has no main shopping activity which could be relocated
		if (shoppingIndices != null) {
			for (int index : shoppingIndices) {
				/*
				 * The Probabilities for the Shopping Zones depends on the Home Zone.
				 * The first Activity in each plan is being at home. Therefore we use
				 * "planElements.get(0)".
				 */
				relocateActivity((Activity) planElements.get(0), (Activity) planElements.get(index));
			}		
		}
	}
	
	private void relocateActivity(Activity homeActivity, Activity shoppingActivity) {
		Id homeLinkId = homeActivity.getLinkId();
		
		int homeTAZ = zoneMapping.getLinkTAZ(homeLinkId);
		
		Map<Integer, Double> probabilities = locationChoiceProbabilityCreator.getFromZoneProbabilities(homeTAZ);
		
		int newZone = selectZoneByProbability(probabilities);
		Id newLinkId = selectLinkByZone(newZone);
		
		/*
		 * We have only one Facility per Link which has the same Id as the Link itself.
		 */
		ActivityFacility facility = ((MutableScenario)scenario).getActivityFacilities().getFacilities().get(newLinkId);

		/*
		 * Replace LinkId, FacilityId and the Coordinate
		 */
		((Activity) shoppingActivity).setLinkId(newLinkId);
		((Activity) shoppingActivity).setFacilityId(newLinkId);
		((Activity) shoppingActivity).setCoord(facility.getCoord());
	}
	
	private int selectZoneByProbability(Map<Integer, Double> probabilities) {
		double sumProbability = 0.0;
		double randomProbability = random.nextDouble();
		
		int counter = 0;
		for (Entry<Integer, Double> entry : probabilities.entrySet()) {
			counter++;
			
			/*
			 * If its the last entry we can return its key.
			 */
			if (counter == probabilities.size()) return entry.getKey();
			
			if (randomProbability <= sumProbability + entry.getValue()) return entry.getKey();
			
			sumProbability = sumProbability + entry.getValue();
		}
		
		return -1;
	}
	
	private Id selectLinkByZone(int TAZ) {
		List<ActivityFacility> list = facilityLocationMap.get(TAZ);
		
		if (list == null) {
			log.warn("Zone " + TAZ + " has no mapped Links!");
			return null;
		}
		
		return list.get(this.random.nextInt(list.size())).getLinkId();
	}
}
