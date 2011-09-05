/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityChoiceSetGenerator.java
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
package playground.johannes.studies.coopsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.sim.gis.MatsimCoordUtils;

/**
 * @author illenberger
 *
 */
public class FacilityChoiceSetGenerator {

	private final DistanceCalculator calculator;
	
	private final int numChoices;
	
	private final double gamma;
	
	private final Random random;
	
	public FacilityChoiceSetGenerator(double gamma, int numChoices, Random random) {
		this(gamma, numChoices, random, OrthodromicDistanceCalculator.getInstance());
	}
	
	public FacilityChoiceSetGenerator(double gamma, int numChoices, Random random, DistanceCalculator calculator) {
		this.gamma = gamma;
		this.numChoices = numChoices;
		this.random = random;
		this.calculator = calculator;
	}
	
	public Map<SocialVertex, List<Id>> generate(SocialGraph graph, ActivityFacilities facilities, String type) {
		List<ActivityFacility> facList = filterFacilities(facilities, type);
		
		Map<SocialVertex, List<Id>> choiceSets = new HashMap<SocialVertex, List<Id>>(graph.getVertices().size());
		for(SocialVertex ego : graph.getVertices()) {
			/*
			 * calculate normalization
			 */
			double norm = 0;
			for(ActivityFacility facility : facList) {
				double d = calculator.distance(ego.getPoint(), MatsimCoordUtils.coordToPoint(facility.getCoord()));
				norm += Math.pow(d, gamma);
			}
			/*
			 * draw facilities
			 */
			List<Id> choiceSet = new ArrayList<Id>((int) (numChoices * 1.5));
			for(ActivityFacility facility : facList) {
				double d = calculator.distance(ego.getPoint(), MatsimCoordUtils.coordToPoint(facility.getCoord()));
				double p = numChoices/norm * Math.pow(d, gamma);
				if(random.nextDouble() < p)
					choiceSet.add(facility.getId());
			}
			
			choiceSets.put(ego, choiceSet);
		}
		
		return choiceSets;
	}
	
	private List<ActivityFacility> filterFacilities(ActivityFacilities facilities, String type) {
		List<ActivityFacility> facList = new ArrayList<ActivityFacility>(facilities.getFacilities().size());
		
		for(Entry<Id, ? extends ActivityFacility> entry : facilities.getFacilities().entrySet()) {
			ActivityFacility facility = entry.getValue();
			for(ActivityOption option : facility.getActivityOptions().values()) {
				if(type == null || option.getType().equals(type)) {
					facList.add(facility);
					break;
				}
			}
		}
		
		return facList;
	}
}
