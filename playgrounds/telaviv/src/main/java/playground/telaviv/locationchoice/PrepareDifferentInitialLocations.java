/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareDifferentInitialLocations.java
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

package playground.telaviv.locationchoice;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * In the initial population, secondary activity locations have already been selected
 * based on the Tel Aviv Model, i.e. they are already distributed quite good. Here, we
 * replace their location by the one closest to their previous activity. Then, we re-run
 * the simulation to check whether the location choice code still produces the same
 * distribution.
 * 
 * @author cdobler
 */
public class PrepareDifferentInitialLocations {

	public static String LEISURE = "leisure";
	public static String SHOPPING = "shopping";
	
	private QuadTree<Id<ActivityFacility>> leisureQuadTree;
	private QuadTree<Id<ActivityFacility>> shoppingQuadTree;
	
	public PrepareDifferentInitialLocations(final Scenario scenario, final String prefixToSkip) {
		
		this.leisureQuadTree = this.buildQuadTree(scenario, LEISURE);
		this.shoppingQuadTree = this.buildQuadTree(scenario, SHOPPING);
		
		Counter counter = new Counter("# prepared persons: ");
		LocationShifter locationShifter = new LocationShifter(scenario, this.leisureQuadTree, this.shoppingQuadTree);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			// to skip tta agents
			if (prefixToSkip != null) if (person.getId().toString().startsWith(prefixToSkip)) continue;
			
			for (Plan plan : person.getPlans()) locationShifter.run(plan);
			counter.incCounter();
		}
		counter.printCounter();
	}
	
	private QuadTree<Id<ActivityFacility>> buildQuadTree(Scenario scenario, String type) {
		
		Map<Id<ActivityFacility>, ActivityFacility> facilities = scenario.getActivityFacilities().getFacilitiesForActivityType(type);
		
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		
		for (ActivityFacility facility : facilities.values()) {
			Coord coord = facility.getCoord();
			double x = coord.getX();
			double y = coord.getY();
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
		}
		QuadTree<Id<ActivityFacility>> quadTree = new QuadTree<Id<ActivityFacility>>(minX, minY, maxX, maxY);
		for (ActivityFacility facility : facilities.values()) {
			Coord coord = facility.getCoord();
			double x = coord.getX();
			double y = coord.getY();
			quadTree.put(x, y, facility.getId());
		}
		
		return quadTree;
	}
	
	private static class LocationShifter implements PlanAlgorithm {

		private final Scenario scenario;
		private final QuadTree<Id<ActivityFacility>> leisureQuadTree;
		private final QuadTree<Id<ActivityFacility>> shoppingQuadTree;
		
		public LocationShifter(Scenario scenario, QuadTree<Id<ActivityFacility>> leisureQuadTree, 
				QuadTree<Id<ActivityFacility>> shoppingQuadTree) {
			this.scenario = scenario;
			this.leisureQuadTree = leisureQuadTree;
			this.shoppingQuadTree = shoppingQuadTree;
		}
		
		@Override
		public void run(Plan plan) {
			
			List<PlanElement> planElements = plan.getPlanElements();
			if (planElements.size() <= 1) return;
			
			Id lastFacilityId = ((Activity) planElements.get(0)).getFacilityId();
			
			for (int i = 2; i < planElements.size(); i++) {
				PlanElement planElement = planElements.get(i);
				if (planElement instanceof Leg) continue;
				
				Activity activity = (Activity) planElement;
				String type = activity.getType();
				
				if (LEISURE.equals(type)) {
					this.relocateActivity(scenario, activity, lastFacilityId, this.leisureQuadTree);
				} else if (SHOPPING.equals(type)) {
					this.relocateActivity(scenario, activity, lastFacilityId, this.shoppingQuadTree);
				}
				
				// update lastfacilityId
				lastFacilityId = activity.getFacilityId();
			}
			
		}
		
		private void relocateActivity(Scenario scenario, Activity activity, Id<ActivityFacility> lastFacilityId, 
				QuadTree<Id<ActivityFacility>> quadTree) {
			
			// get the position of the last performed activity
			ActivityFacility lastFacility = scenario.getActivityFacilities().getFacilities().get(lastFacilityId);
			Coord coord = lastFacility.getCoord();
			double x = coord.getX();
			double y = coord.getY();
			
			Id<ActivityFacility> newFacilityId = quadTree.get(x, y);
			ActivityFacility newFacility = scenario.getActivityFacilities().getFacilities().get(newFacilityId);
			
			// replace activity location
			((ActivityImpl) activity).setLinkId(newFacility.getLinkId());
			((ActivityImpl) activity).setFacilityId(newFacility.getId());
		}
	}	
}