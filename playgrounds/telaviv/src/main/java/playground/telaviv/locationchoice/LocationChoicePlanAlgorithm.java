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

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.telaviv.facilities.Emme2FacilitiesCreator;
import playground.telaviv.zones.ZoneMapping;

/*
 * Use fixed probabilities over the day which are read from a text file.
 */
public class LocationChoicePlanAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(LocationChoicePlanAlgorithm.class);
	
	private Scenario scenario;
	private Emme2FacilitiesCreator facilitiesCreator = null;
	private LocationChoiceProbabilityCreator locationChoiceProbabilityCreator = null;
	private ZoneMapping zoneMapping = null;
	private Map<Id, List<Integer>> shoppingActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
		
	private Random random = MatsimRandom.getLocalInstance();
	
	LocationChoicePlanAlgorithm(Scenario scenario, Emme2FacilitiesCreator facilitiesCreator, 
			LocationChoiceProbabilityCreator locationChoiceProbabilityCreator,
			ZoneMapping zoneMapping, Map<Id, List<Integer>> shoppingActivities) {
		this.scenario = scenario;
		this.facilitiesCreator = facilitiesCreator;
		this.locationChoiceProbabilityCreator = locationChoiceProbabilityCreator;
		this.zoneMapping = zoneMapping;
		this.shoppingActivities = shoppingActivities;
	}
	
	/*
	 * Searches for Shopping Activities that can be replaced.
	 * If no ones are found no replanning is neccessary and therefore
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
		ActivityFacility facility = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(newLinkId);

		/*
		 * Replace LinkId, FacilityId and the Coordinate
		 */
		((ActivityImpl) shoppingActivity).setLinkId(newLinkId);
		((ActivityImpl) shoppingActivity).setFacilityId(newLinkId);
		((ActivityImpl) shoppingActivity).setCoord(facility.getCoord());
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
	
	/*
	 * The link is selected randomly but the length of the links 
	 * is used to weight the probability.
	 */
	private Id selectLinkByZone(int TAZ) {		
		List<Id> linkIds = facilitiesCreator.getLinkIdsInZoneForFacilites(TAZ);
		
		if (linkIds == null) {
			log.warn("Zone " + TAZ + " has no mapped Links!");
			return null;
		}
		
		double totalLength = 0;
		for (Id id : linkIds) {
			Link link = zoneMapping.getNetwork().getLinks().get(id);
			totalLength = totalLength + link.getLength();
		}
		
		double[] probabilities = new double[linkIds.size()];
		double sumProbability = 0.0;
		for (int i = 0; i < linkIds.size(); i++) {
			Link link = zoneMapping.getNetwork().getLinks().get(linkIds.get(i));
			double probability = link.getLength() / totalLength;
			probabilities[i] = sumProbability + probability;
			sumProbability = probabilities[i];
		}
		
		double randomProbability = random.nextDouble();
		for (int i = 0; i < linkIds.size() - 1; i++) {
			if (randomProbability <= probabilities[i + 1]) return linkIds.get(i);
		}
		return null;
	}
}
