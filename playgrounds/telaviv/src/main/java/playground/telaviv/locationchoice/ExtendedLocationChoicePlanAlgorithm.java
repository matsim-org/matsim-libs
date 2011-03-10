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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.telaviv.facilities.Emme2FacilitiesCreator;
import playground.telaviv.zones.ZoneMapping;

/*
 * Calculates the probabilities dynamically, depending on the travel times
 * in the scenario and the departure time. 
 */
public class ExtendedLocationChoicePlanAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(ExtendedLocationChoicePlanAlgorithm.class);
	
	private Scenario scenario;
	private Emme2FacilitiesCreator facilitiesCreator = null;
	private ExtendedLocationChoiceProbabilityCreator extendedLocationChoiceProbabilityCreator = null;
	private ZoneMapping zoneMapping = null;
	private Map<Id, List<Integer>> shoppingActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	private Map<Id, List<Integer>> otherActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	private Map<Id, List<Integer>> workActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	private Map<Id, List<Integer>> educationActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	
	private int[] types = new int[]{0, 1, 2, 3}; // Shop, Other, Work, Education
	
	private Random random = MatsimRandom.getLocalInstance();
	
	public ExtendedLocationChoicePlanAlgorithm(Scenario scenario, Emme2FacilitiesCreator facilitiesCreator, 
			ExtendedLocationChoiceProbabilityCreator extendedLocationChoiceProbabilityCreator,
			ZoneMapping zoneMapping, Map<Id, List<Integer>> shoppingActivities, Map<Id, List<Integer>> otherActivities,
			Map<Id, List<Integer>> workActivities, Map<Id, List<Integer>> educationActivities) {
		this.scenario = scenario;
		this.facilitiesCreator = facilitiesCreator;
		this.extendedLocationChoiceProbabilityCreator = extendedLocationChoiceProbabilityCreator;
		this.zoneMapping = zoneMapping;
		this.shoppingActivities = shoppingActivities;
		this.otherActivities = otherActivities;
		this.workActivities = workActivities;
		this.educationActivities = educationActivities;
	}
	
	/*
	 * Searches for Shopping Activities that can be replaced.
	 * If no ones are found no replanning is neccessary and therefore
	 * false is returned.
	 */
	@Override
	public void run(Plan plan) {
		List<Integer> shoppingIndices = shoppingActivities.get(plan.getPerson().getId());
		List<Integer> otherIndices = otherActivities.get(plan.getPerson().getId());
		List<Integer> workIndices = workActivities.get(plan.getPerson().getId());
		List<Integer> educationIndices = educationActivities.get(plan.getPerson().getId());

		List<PlanElement> planElements = plan.getPlanElements();

		relocate(planElements, shoppingIndices, types[0]);
		relocate(planElements, otherIndices, types[1]);
		relocate(planElements, workIndices, types[2]);
		relocate(planElements, educationIndices, types[3]);
		
//		// if List is null, the Person has no main shopping activity
//		if (shoppingIndices != null) {
//			for (int index : shoppingIndices) {
//				Activity homeActivity = (Activity) planElements.get(0);
//				
//				double departureTime = 0.0;
//				
//				/*
//				 * Get Departure Time from previous Home Activity. 
//				 */
//				if (((Activity)planElements.get(index - 2)).getType().equals(homeActivity.getType())) {
//					departureTime = ((Activity)planElements.get(index - 2)).getEndTime(); 
//				}
//				else if (((Activity)planElements.get(index - 4)).getType().equals(homeActivity.getType())) {
//					departureTime = ((Activity)planElements.get(index - 4)).getEndTime(); 
//				}
//				
//				/*
//				 * The Probabilities for the Shopping Zones depends on the Home Zone.
//				 * The first Activity in each plan is being at home. Therefore we use
//				 * "planElements.get(0)".
//				 */
//				relocateActivity(types[0], homeActivity, (Activity) planElements.get(index), departureTime);
//			}			
//		}				
	}
	
	private void relocate(List<PlanElement> planElements, List<Integer> indices, int type) {
		if (indices == null) return;
		
		for (int index : indices) {
			Activity homeActivity = (Activity) planElements.get(0);
			
			double departureTime = 0.0;
			
			/*
			 * Get Departure Time from previous Home Activity. 
			 */
			if (((Activity)planElements.get(index - 2)).getType().equals(homeActivity.getType())) {
				departureTime = ((Activity)planElements.get(index - 2)).getEndTime(); 
			}
			else if (((Activity)planElements.get(index - 4)).getType().equals(homeActivity.getType())) {
				departureTime = ((Activity)planElements.get(index - 4)).getEndTime(); 
			}
			
			/*
			 * The Probabilities for the Shopping Zones depends on the Home Zone.
			 * The first Activity in each plan is being at home. Therefore we use
			 * "planElements.get(0)".
			 */
			relocateActivity(type, homeActivity, (Activity) planElements.get(index), departureTime);
		}			
	}
	
	private void relocateActivity(int type, Activity homeActivity, Activity activityToRelocate, double departureTime) {
		Id homeLinkId = homeActivity.getLinkId();
		
		int homeTAZ = zoneMapping.getLinkTAZ(homeLinkId);
		
		Tuple<int[], double[]> probabilities = extendedLocationChoiceProbabilityCreator.getFromZoneProbabilities(type, homeTAZ, departureTime);
		
		int newZone = selectZoneByProbability(probabilities);
		Id newLinkId = selectLinkByZone(newZone);
		
		/*
		 * We have only one Facility per Link which has the same Id as the Link itself.
		 */
		ActivityFacility facility = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(newLinkId);

		/*
		 * Replace LinkId, FacilityId and the Coordinate
		 */
		((ActivityImpl) activityToRelocate).setLinkId(newLinkId);
		((ActivityImpl) activityToRelocate).setFacilityId(newLinkId);
		((ActivityImpl) activityToRelocate).setCoord(facility.getCoord());
		
		/*
		 * If it is an Education Activity we probably have to update the activity type.
		 * Each Zone has only one of the three education types. Therefore we select the
		 * new zone and then set education type of that zone in the activity.
		 */
		if (type == 3) {
			String educationType = null;
			for (ActivityOption activityOption : facility.getActivityOptions().values()) {
				if (activityOption.getType().toLowerCase().contains("university")) {
					educationType = activityOption.getType();
					break;
				}
				else if (activityOption.getType().toLowerCase().contains("highschool")) {
					educationType = activityOption.getType();
					break;
				}
				else if (activityOption.getType().toLowerCase().contains("elementaryschool")) {
					educationType = activityOption.getType();
					break;
				}
			}
			/*
			 * Not every Zone has education facilities. If such a zone was chosen, 
			 * we select another one. 
			 */
			if (educationType != null) ((ActivityImpl) activityToRelocate).setType(educationType);
			else relocateActivity(type, homeActivity, activityToRelocate, departureTime);
		}
	}
	
	private int selectZoneByProbability(Tuple<int[], double[]> tuple) {
		double sumProbability = 0.0;
		double randomProbability = random.nextDouble();
		
		int[] indices = tuple.getFirst();
		double[] probabilities = tuple.getSecond();
		
		int counter = 0;
		for (double probability : probabilities) {
			/*
			 * If its the last entry we can return its key.
			 */
			if (counter + 1 == indices.length) return indices[counter];
			
			if (randomProbability <= sumProbability + probability) return indices[counter];
			
			sumProbability = sumProbability + probability;
			counter++;
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