/* *********************************************************************** *
 * project: org.matsim.*
 * EnsureActivityReachability.java
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

package org.matsim.contrib.multimodal.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.Facility;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Agents try to reach an Activity on a Link which does not support
 * the transport mode of the Leg.
 * 
 * PT is currently possible on car/bike/walk links, therefore we never 
 * have to relocate a PT leg.
 * 
 * If an Activity Location is changed, the Routes of its to and from Legs
 * are set to null.
 */
class EnsureActivityReachability extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(EnsureActivityReachability.class);
	
	private final String allModes = "allModes";
	
	private final Scenario scenario;
	private final MultiModalConfigGroup multiModalConfigGroup;
	
	private final Counter relocateCounter = new Counter("Number of relocated activities: ");
	
	private Map<String, QuadTree<Facility>> facilityQuadTrees;
	private Map<String, QuadTree<Link>> linkQuadTrees;
	
	public EnsureActivityReachability(Scenario scenario) {
		this.scenario = scenario;
		this.multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		
		buildLinkQuadTrees();
		buildFacilityQuadTrees();
	}
	
	public void printRelocateCount() {
		this.relocateCounter.printCounter();
	}
	
	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	@Override
	public void run(Plan plan) {
		int index = 0;
		int planElements = plan.getPlanElements().size();
		
		// if the plan contains no legs we have noting more to do
		if (planElements <= 1) return; 
		
		for (PlanElement planElement : plan.getPlanElements()) {
			
			if (planElement instanceof Activity) {
				boolean relocateActivity = false;
				Set<String> requiredModes = new HashSet<>();
				
				Activity activity = (Activity) planElement;
				
				// if no LinkId is available we try to get one
				if (activity.getLinkId() == null) {
					// if also no FacilityId we cannot assign a link -> do nothing
					if (activity.getFacilityId() == null) continue;
					
					/*
					 *  A FacilityId was found. Try to get Facility and assign its LinkId to
					 *  the Activity. It that is not possible, nothing is done.
					 */
					else {
						Facility facility = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId());
						if (facility != null) {
							if (facility.getLinkId() != null) {
								((ActivityImpl)activity).setLinkId(facility.getLinkId());
							}
							else continue;
						}
						else continue;
					}
				}
				
				Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());
				
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				
				// PT is currently allowed on all kinds of links
				allowedModes.add(TransportMode.pt);
				
				// if car mode is allowed, also ride mode is allowed
				if (allowedModes.contains(TransportMode.car)) allowedModes.add(TransportMode.ride);
				
				// if its the first Activity
				if (index == 0) {
					Leg nextLeg = (Leg) plan.getPlanElements().get(index + 1);
					if (!allowedModes.contains(nextLeg.getMode())) {
						relocateActivity = true;
						requiredModes.add(nextLeg.getMode());
						nextLeg.setRoute(null);
					}
				}
				// if it is the last Activity
				else if (index == planElements - 1) {
					Leg previousLeg = (Leg) plan.getPlanElements().get(index - 1);
					if (!allowedModes.contains(previousLeg.getMode())) {
						relocateActivity = true;
						requiredModes.add(previousLeg.getMode());
						previousLeg.setRoute(null);
					}
				}
				// in between Activity
				else {
					
					Leg previousLeg = (Leg) plan.getPlanElements().get(index - 1);
					Leg nextLeg = (Leg) plan.getPlanElements().get(index + 1);
					
					if (!allowedModes.contains(nextLeg.getMode()) || !allowedModes.contains(previousLeg.getMode())) {
						relocateActivity = true;
						requiredModes.add(nextLeg.getMode());
						requiredModes.add(previousLeg.getMode());
						nextLeg.setRoute(null);
						previousLeg.setRoute(null);
					}
				}			
				
				// if we have to relocate the Activity
				if (relocateActivity) {
					
					/*
					 * If the modes of from- and toLeg differs we select a facility
					 * on a link which supports all transport modes.
					 * If the Activity is located at a Facility, we move it to another
					 * Facility, if its located at a Link, we move it to another Link. 
					 */
					if (activity.getFacilityId() != null) {
						Facility newFacility;
						if (requiredModes.size() > 1) {
							newFacility = this.facilityQuadTrees.get(this.allModes).get(activity.getCoord().getX(), activity.getCoord().getY());
						} else {
							String[] modes = new String[1];
							requiredModes.toArray(modes);
							newFacility = this.facilityQuadTrees.get(modes[0]).get(activity.getCoord().getX(), activity.getCoord().getY());
						}
							
						if (newFacility != null) {
							((ActivityImpl) activity).setFacilityId(newFacility.getId());
							((ActivityImpl) activity).setLinkId(newFacility.getLinkId());
							((ActivityImpl) activity).setCoord(newFacility.getCoord());
							relocateCounter.incCounter();
						}
						else {
							log.error("Could not relocate Activity");
						}				
					} else if (activity.getLinkId() != null) {
						Link newLink;
						if (requiredModes.size() > 1) {
							newLink = this.linkQuadTrees.get(this.allModes).get(activity.getCoord().getX(), activity.getCoord().getY());
						} else {
							String[] modes = new String[1];
							requiredModes.toArray(modes);
							newLink = this.linkQuadTrees.get(modes[0]).get(activity.getCoord().getX(), activity.getCoord().getY());
						}
						
						if (newLink != null) {
							((ActivityImpl) activity).setLinkId(newLink.getId());
							((ActivityImpl) activity).setCoord(newLink.getCoord());
							relocateCounter.incCounter();
						}
						else {
							log.error("Could not relocate Activity");
						}
					} else {
						log.error("Could not relocate Activity");
					}	
						
				}
			}				
			index++;
		}
	}
	
	private void buildLinkQuadTrees() {

		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getCoord().getX() < minx) { minx = link.getCoord().getX(); }
			if (link.getCoord().getY() < miny) { miny = link.getCoord().getY(); }
			if (link.getCoord().getX() > maxx) { maxx = link.getCoord().getX(); }
			if (link.getCoord().getY() > maxy) { maxy = link.getCoord().getY(); }
		}
		
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		log.info("QuadTrees: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		
		Set<String> modes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
		this.linkQuadTrees = new HashMap<>();
		for (String mode : modes) {
			this.linkQuadTrees.put(mode, new QuadTree<Link>(minx, miny, maxx, maxy));
		}
		this.linkQuadTrees.put(this.allModes, new QuadTree<Link>(minx, miny, maxx, maxy));
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			
			Set<String> allowedModes = link.getAllowedModes();
			for (String allowedMode : allowedModes) {
				if (modes.contains(allowedMode)) {
					this.linkQuadTrees.get(allowedMode).put(link.getCoord().getX(), link.getCoord().getY(), link);
				}
			}
			if (allowedModes.containsAll(modes)) {
				this.linkQuadTrees.get(this.allModes).put(link.getCoord().getX(), link.getCoord().getY(), link);
			}
		}

		for (String mode : modes) {
			log.info("Found " + this.linkQuadTrees.get(mode).size() + "  links where mode " + mode + " is allowed.");
		}
		log.info("Found " + this.linkQuadTrees.get(this.allModes).size() + "  links where all modes are allowed.");
	}
	
	private void buildFacilityQuadTrees() {

		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (Facility facility : scenario.getActivityFacilities().getFacilities().values()) {
			if (facility.getCoord().getX() < minx) { minx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() < miny) { miny = facility.getCoord().getY(); }
			if (facility.getCoord().getX() > maxx) { maxx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() > maxy) { maxy = facility.getCoord().getY(); }
		}
		
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		log.info("QuadTrees: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		
		Set<String> modes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
		this.facilityQuadTrees = new HashMap<>();
		for (String mode : modes) {
			this.facilityQuadTrees.put(mode, new QuadTree<Facility>(minx, miny, maxx, maxy));
		}
		this.facilityQuadTrees.put(this.allModes, new QuadTree<Facility>(minx, miny, maxx, maxy));
		
		for (Facility facility : scenario.getActivityFacilities().getFacilities().values()) {
			Link link = scenario.getNetwork().getLinks().get(facility.getLinkId());
			
			Set<String> allowedModes = link.getAllowedModes();
			for (String allowedMode : allowedModes) {
				if (modes.contains(allowedMode)) {
					this.facilityQuadTrees.get(allowedMode).put(facility.getCoord().getX(), link.getCoord().getY(), facility);
				}
			}
			if (allowedModes.containsAll(modes)) {
				this.facilityQuadTrees.get(this.allModes).put(facility.getCoord().getX(), link.getCoord().getY(), facility);
			}
		}
		
		for (String mode : modes) {
			log.info("Found " + this.facilityQuadTrees.get(mode).size() + "  facilities where mode " + mode + " is allowed.");
		}
		log.info("Found " + this.facilityQuadTrees.get(this.allModes).size() + "  facilities where all modes are allowed.");
	}
}
