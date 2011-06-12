/* *********************************************************************** *
 * project: org.matsim.*
 * RelocateActivities.java
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

package playground.christoph.energyflows.population;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;

/*
 * Relocate Activities that should be performed in Facilities within the
 * City of Zurich which are replaced by new ones.
 */
public class RelocateActivities {

	final private static Logger log = Logger.getLogger(RelocateActivities.class);
	
	private Random random = MatsimRandom.getLocalInstance();
	private double coordinateFuzzyValue = 25.0;
	
	private Knowledges knowledges;
	private ActivityFacilities zurichFacilities;	
	private String[] activityTypes = {"home", "shop", "education_higher", "education_kindergarten", "education_other", 
			"education_primary", "education_secondary", "leisure", "work_sector2", "work_sector3"};
	
	private Map<String, QuadTree<Id>> quadTrees;
	private Map<Id, Map<String, Double>> usedCapacities;
	
	public RelocateActivities(ActivityFacilities zurichFacilities, Knowledges knowledges) {
		this.zurichFacilities = zurichFacilities;
		this.knowledges = knowledges;
		
		log.info("building quad trees...");
		quadTrees = new HashMap<String, QuadTree<Id>>();
		for (String activityType : activityTypes) quadTrees.put(activityType, createQuadTree(activityType));

		log.info("done.");
		
		usedCapacities = new HashMap<Id, Map<String, Double>>();
		for (ActivityFacility facility : zurichFacilities.getFacilities().values()) {
			Map<String, Double> map = new HashMap<String, Double>();
			usedCapacities.put(facility.getId(), map);

			for (String activityType : facility.getActivityOptions().keySet()) {
				map.put(activityType, 0.0);
			}
		}
	}
	
	private QuadTree<Id> createQuadTree(String activityType) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (ActivityFacility facility : zurichFacilities.getFacilities().values()) {
			// if the facility does not offer the given activity type
			if (facility.getActivityOptions().get(activityType) == null) continue;
			
			if (facility.getCoord().getX() < minx) { minx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() < miny) { miny = facility.getCoord().getY(); }
			if (facility.getCoord().getX() > maxx) { maxx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() > maxy) { maxy = facility.getCoord().getY(); }
		}
		
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		log.info(activityType + " xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
				
		QuadTree<Id> quadTree = new QuadTree<Id>(minx, miny, maxx, maxy);
		
		log.info("filling facilities quad tree...");
		for (ActivityFacility facility : zurichFacilities.getFacilities().values()) {
			if (facility.getActivityOptions().get(activityType) == null) continue;
			
			quadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility.getId());
		}
		log.info("done.");
		
		return quadTree;
	}
		
	public void relocateActivities(Population population, Set<Id> personIds, Set<Id> facilitiesToRemove) {
		
		log.info("relocating activities...");
		Counter counter = new Counter("relocated activities... ");
		for (Id personId : personIds) {
			
			// If home or work activities are moved, we select a new facility only once and the reuse its Id.
			Id homeFacilityId = null;
			Id workFacilityId = null;
			String workSectorType = null;
			
			Person person = population.getPersons().get(personId);
			
			for (Plan plan : person.getPlans()) {
				
				for (int i = 0; i < plan.getPlanElements().size(); i++) {
					PlanElement planElement = plan.getPlanElements().get(i);
					
					if (planElement instanceof Leg) continue;
					else {
						Activity activity = (Activity) planElement;
						if (facilitiesToRemove.contains(activity.getFacilityId())) {	
														
							if (activity.getType().equals("home") && homeFacilityId != null) {
								relocateActivityTo(activity, homeFacilityId);
							} else if (activity.getType().equals("work_sector2") && workFacilityId != null) {
								activity.setType(workSectorType);
								relocateActivityTo(activity, workFacilityId);
							} else if (activity.getType().equals("work_sector3") && workFacilityId != null) {
								activity.setType(workSectorType);
								relocateActivityTo(activity, workFacilityId);
							} else {
								relocateActivity(activity);
								adaptKnowledge(person, activity);
								counter.incCounter();
								
								// check capacity and remove facility from quad tree if the capacity limit is reached
								checkCapacity(activity.getFacilityId(), activity.getType());								

								// if it is a home or work activity we store the new facilityId to be able to reuse it
								if (activity.getType().equals("home")) {
									homeFacilityId = activity.getFacilityId();
								} else if (activity.getType().equals("work_sector2") || activity.getType().equals("work_sector3")) {
									workFacilityId = activity.getFacilityId();
									workSectorType = activity.getType();
								}
							}
							
							// Reset routes to and from this Activity
							if (i > 0) {
								Leg leg = (Leg) plan.getPlanElements().get(i - 1);
								leg.setRoute(null);
							}
							if (i < plan.getPlanElements().size() - 1) {
								Leg leg = (Leg) plan.getPlanElements().get(i + 1);
								leg.setRoute(null);
							}
						}
					}
				}
			}
		}
		counter.printCounter();
		log.info("done.");
	}
	
	private void relocateActivity(Activity activity) {
		double x = activity.getCoord().getX() + random.nextDouble()*coordinateFuzzyValue;
		double y = activity.getCoord().getY() + random.nextDouble()*coordinateFuzzyValue;

		String activityType = activity.getType();
		
		Id relocatedFacilityId;
		ActivityFacility relocatedFacility;
		Map<String, Double> map;
		
		/*
		 * If it is a work activity select the nearest sector2 or sector3 work location.
		 */
		if (activityType.equals("work_sector2") || activityType.equals("work_sector3")) {
			Id sector2Id = quadTrees.get("work_sector2").get(x, y);
			Id sector3Id = quadTrees.get("work_sector3").get(x, y);
			ActivityFacility sector2Facility = zurichFacilities.getFacilities().get(sector2Id);
			ActivityFacility sector3Facility = zurichFacilities.getFacilities().get(sector3Id);
			
			Coord coord = new CoordImpl(x,y);
			double sector2Distance = CoordUtils.calcDistance(sector2Facility.getCoord(), coord);
			double sector3Distance = CoordUtils.calcDistance(sector3Facility.getCoord(), coord);
			
			if (sector2Distance < sector3Distance) {
				relocatedFacilityId = sector2Id;
				activity.setType("work_sector2");
				activityType = "work_sector2";
			}
			else {
				relocatedFacilityId = sector3Id;
				activity.setType("work_sector3");
				activityType = "work_sector3";
			}
		} else {
			relocatedFacilityId = quadTrees.get(activity.getType()).get(x, y);
		}
		relocatedFacility = zurichFacilities.getFacilities().get(relocatedFacilityId);
		map = usedCapacities.get(relocatedFacility.getId());			
		
		((ActivityImpl) activity).setFacilityId(relocatedFacilityId);
		((ActivityImpl) activity).setLinkId(relocatedFacility.getLinkId());
		((ActivityImpl) activity).setCoord(relocatedFacility.getCoord());
				
		/*
		 *  Increase usage counter
		 *  Home and Work activities increase the used capacity by one (could this become a problem with part time workers?)
		 *  For all other activity types: calculate the share of used capacity (time spent at facility / facility opening time)
		 */
		if (activityType.equals("home") || activityType.equals("work_sector2") || activityType.equals("work_sector3")) {
			map.put(activity.getType(), map.get(activity.getType()) + 1);
		} else {
			ActivityOption activityOption = relocatedFacility.getActivityOptions().get(activityType);
			Set<OpeningTime> openingTimes = activityOption.getOpeningTimes(DayType.mon);
			
			double time = 0.0;
			for (OpeningTime openingTime : openingTimes) time += openingTime.getEndTime() - openingTime.getStartTime();
			
			double duration = activity.getEndTime() - activity.getStartTime();
			
			double additionalUseage =  duration/time;
			if (additionalUseage > 1.0) additionalUseage = 1.0;
			map.put(activity.getType(), map.get(activity.getType()) + additionalUseage);
		}
	}
	
	// only relocate but do no increase capacity usage
	private void relocateActivityTo(Activity activity, Id relocatedFacilityId) {
		
		Facility relocatedFacility = zurichFacilities.getFacilities().get(relocatedFacilityId); 
		
		((ActivityImpl) activity).setFacilityId(relocatedFacilityId);
		((ActivityImpl) activity).setLinkId(relocatedFacility.getLinkId());
		((ActivityImpl) activity).setCoord(relocatedFacility.getCoord());
	}
	
	/*
	 * The given activity has been relocated. We use its activity type and its new location
	 * to update the persons knowledge.
	 */
	private void adaptKnowledge(Person person, Activity activity) {
		
		// adapt the knowledge - ActivityOptions cannot be edited, therefore replace them
		KnowledgeImpl knowledge = knowledges.getKnowledgesByPersonId().get(person.getId());
		ActivityFacility facility = zurichFacilities.getFacilities().get(activity.getFacilityId()); 
		
		List<ActivityOptionImpl> list = knowledge.getActivities(activity.getType());
		if (list == null) return;
		
		for (ActivityOptionImpl activityOption : list) {
			ActivityOptionImpl newActivityOption = new ActivityOptionImpl(activity.getType(), (ActivityFacilityImpl) facility);
			boolean isPrimary = knowledge.isPrimary(activity.getType(), activityOption.getFacilityId());
			knowledge.removeActivity(activityOption);
			knowledge.addActivityOption(newActivityOption, isPrimary);
		}
	}
	
	private void checkCapacity(Id facilityId, String activityType) {
		
		ActivityFacility facility = zurichFacilities.getFacilities().get(facilityId);
		Map<String, Double> map = usedCapacities.get(facilityId);
		double usage = map.get(activityType);
		double availableCapacity = facility.getActivityOptions().get(activityType).getCapacity();
		
		// available capacity is not exceeded, therefore we can continue
		if (activityType.equals("home") || activityType.equals("work_sector2") || activityType.equals("work_sector3")) {
			// if still at least one place left
			if (usage + 1.0 <= availableCapacity) return;			
		} else {
			// we do not want facilities to be filled more than 75% in average -> avoid overcrowding during peaks
			if (usage + 1.0 <= 0.75 * availableCapacity) return;
		}
		
		// otherwise remove facility from quad tree 
		QuadTree<Id> quadTree = quadTrees.get(activityType);
		quadTree.remove(facility.getCoord().getX(), facility.getCoord().getY(), facility.getId());
		
//		// if it is a work facility which is removed from the quad tree: also remove it from the merged work quad tree
//		if (activityType.equals("work_sector2") || activityType.equals("work_sector3")) {
//			quadTrees.get("work").remove(facility.getCoord().getX(), facility.getCoord().getY(), facility.getId());
//		}
	}
	
	public void checkCapacityUsage() {

		log.info("checking facility capacity usage...");
		Counter exceededCounter = new Counter("number of facilities where the available capacity is exceeded ");
		Counter zeroCounter = new Counter("number of facilities where the used capacity is zero ");
		
		for (ActivityFacility facility : zurichFacilities.getFacilities().values()) {
			Map<String, Double> map = usedCapacities.get(facility.getId());

			for (String activityType : facility.getActivityOptions().keySet()) {
				double usage = map.get(activityType);
				double availableCapacity = facility.getActivityOptions().get(activityType).getCapacity();
				
				if (usage == 0.0) zeroCounter.incCounter();
				
				// available capacity is exceeded
				if (usage > availableCapacity) {
					exceededCounter.incCounter();
					log.info(facility.getId() + " " + activityType + " " + usage + " " + availableCapacity);
				}
			}
		}
		exceededCounter.printCounter();
		zeroCounter.printCounter();
		log.info("done.");
	}
}
