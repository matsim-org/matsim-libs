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

package org.matsim.ptproject.qsim.multimodalsimengine.tools;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

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
public class EnsureActivityReachability extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(EnsureActivityReachability.class);
	
	private Scenario scenario;
		
	private QuadTree<Facility> carFacilityQuadTree;
	private QuadTree<Facility> bikeFacilityQuadTree;
	private QuadTree<Facility> walkFacilityQuadTree;
	private QuadTree<Facility> allModesFacilityQuadTree;
	private QuadTree<Link> carLinkQuadTree;
	private QuadTree<Link> bikeLinkQuadTree;
	private QuadTree<Link> walkLinkQuadTree;
	private QuadTree<Link> allModesLinkQuadTree;	
	
	public EnsureActivityReachability(Scenario scenario) {
		this.scenario = scenario;
		
		buildLinkQuadTrees();
		buildFacilityQuadTrees();
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
				Set<String> requiredModes = new HashSet<String>();
				
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
						Facility facility = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(activity.getFacilityId());
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
				
				Set<String> allowedModes = new HashSet<String>(link.getAllowedModes());
				
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
					boolean carLeg = requiredModes.contains(TransportMode.car);
					boolean bikeLeg = requiredModes.contains(TransportMode.bike);
					boolean walkLeg = requiredModes.contains(TransportMode.walk);
					boolean rideLeg = requiredModes.contains(TransportMode.ride);
					
					/*
					 * If the modes of from- and toLeg differs we select a facility
					 * on a link which supports all transport modes.
					 * If the Activity is located at a Facility, we move it to another
					 * Facility, if its located at a Link, we move it to anoter Link. 
					 */
					if (activity.getFacilityId() != null) {
						Facility newFacility = null;
						if ((carLeg || rideLeg) && (bikeLeg || walkLeg)) {
							newFacility = allModesFacilityQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (carLeg || rideLeg) {
							newFacility = carFacilityQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (bikeLeg) {
							newFacility = bikeFacilityQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (walkLeg) {
							newFacility = walkFacilityQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						}
						
						if (newFacility != null) {
							((ActivityImpl) activity).setFacilityId(newFacility.getId());
							((ActivityImpl) activity).setLinkId(newFacility.getLinkId());
							((ActivityImpl) activity).setCoord(newFacility.getCoord());
						}
						else {
							log.error("Could not relocate Activity");
						}				
					} else if (activity.getLinkId() != null) {
						Link newLink = null;
						if ((carLeg || rideLeg) && (bikeLeg || walkLeg)) {
							newLink = allModesLinkQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (carLeg || rideLeg) {
							newLink = carLinkQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (bikeLeg) {
							newLink = bikeLinkQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (walkLeg) {
							newLink = walkLinkQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						}
						if (newLink != null) {
							((ActivityImpl) activity).setLinkId(newLink.getId());
							((ActivityImpl) activity).setCoord(newLink.getCoord());
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
		
		carLinkQuadTree = new QuadTree<Link>(minx, miny, maxx, maxy);
		bikeLinkQuadTree = new QuadTree<Link>(minx, miny, maxx, maxy);
		walkLinkQuadTree = new QuadTree<Link>(minx, miny, maxx, maxy);
		allModesLinkQuadTree = new QuadTree<Link>(minx, miny, maxx, maxy);
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			
			Set<String> allowedModes = link.getAllowedModes();
			if (allowedModes.contains(TransportMode.car)) carLinkQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
			if (allowedModes.contains(TransportMode.bike)) bikeLinkQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
			if (allowedModes.contains(TransportMode.walk)) walkLinkQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
			
			if (allowedModes.contains(TransportMode.car) && allowedModes.contains(TransportMode.bike) && allowedModes.contains(TransportMode.walk))
				allModesLinkQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);			
		}
		
		log.info("CarLinks:     " + carLinkQuadTree.size());
		log.info("BikeLinks:    " + bikeLinkQuadTree.size());
		log.info("WalkLinks:    " + walkLinkQuadTree.size());
		log.info("AllModeLinks: " + allModesLinkQuadTree.size());
	}
	
	private void buildFacilityQuadTrees() {

		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (Facility facility : ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().values()) {
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
		
		carFacilityQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		bikeFacilityQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		walkFacilityQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		allModesFacilityQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		
		for (Facility facility : ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().values()) {
			Link link = scenario.getNetwork().getLinks().get(facility.getLinkId());
			
			Set<String> allowedModes = link.getAllowedModes();
			if (allowedModes.contains(TransportMode.car)) carFacilityQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			if (allowedModes.contains(TransportMode.bike)) bikeFacilityQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			if (allowedModes.contains(TransportMode.walk)) walkFacilityQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			
			if (allowedModes.contains(TransportMode.car) && allowedModes.contains(TransportMode.bike) && allowedModes.contains(TransportMode.walk))
				allModesFacilityQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);			
		}
		
		log.info("CarFacilities:     " + carFacilityQuadTree.size());
		log.info("BikeFacilities:    " + bikeFacilityQuadTree.size());
		log.info("WalkFacilities:    " + walkFacilityQuadTree.size());
		log.info("AllModeFacilities: " + allModesFacilityQuadTree.size());
	}
}
