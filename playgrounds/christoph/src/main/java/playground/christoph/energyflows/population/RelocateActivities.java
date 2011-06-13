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
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.Desires;

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
	private ActivityFacilities switzerlandFacilities;
	
	private String[] activityTypes = {"home", "shop", "education_higher", "education_kindergarten", "education_other", 
			"education_primary", "education_secondary", "leisure", "work_sector2", "work_sector3"};
	
	private Map<String, QuadTree<Id>> quadTrees;
	private Map<Id, Map<String, Double>> usedCapacities;
	
	public RelocateActivities(ActivityFacilities zurichFacilities, ActivityFacilities switzerlandFacilities, Knowledges knowledges) {
		this.zurichFacilities = zurichFacilities;
		this.switzerlandFacilities = switzerlandFacilities;
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
			
			Person person = population.getPersons().get(personId);
			/*
			 * Mapping:
			 * <activityType in plan, Map<facilityId in plan, Tuple<new facilityId, new activityType>>>
			 */
			Map<String, Map<Id, Tuple<Id, String>>> relocationMapping = new HashMap<String, Map<Id, Tuple<Id, String>>>();
					
			for (Plan plan : person.getPlans()) {
				
				for (int i = 0; i < plan.getPlanElements().size(); i++) {
					PlanElement planElement = plan.getPlanElements().get(i);
					
					if (planElement instanceof Leg) continue;
					else {
						Activity activity = (Activity) planElement;
						if (facilitiesToRemove.contains(activity.getFacilityId())) {
							
							/*
							 * If already a mapping for the current activity type and facility id exists: reuse it.
							 */
							Map<Id, Tuple<Id, String>> relocationMap = relocationMapping.get(activity.getType());
							if (relocationMap != null && relocationMap.get(activity.getFacilityId()) != null) {
								Tuple<Id, String> relocationTuple = relocationMap.get(activity.getFacilityId());
								relocateActivityTo(activity, relocationTuple.getFirst(), relocationTuple.getSecond());
							}
							else {
								Id originalFacilityId = activity.getFacilityId();
								String originalActivityType = activity.getType();
								relocateActivity(activity);
								Id newFacilityId = activity.getFacilityId();
								String newActivityType = activity.getType();
								
								// store mapping
								if (relocationMap == null) {
									relocationMap = new HashMap<Id, Tuple<Id, String>>(); 
									relocationMapping.put(originalActivityType, relocationMap);
								}
								relocationMap.put(originalFacilityId, new Tuple<Id, String>(newFacilityId, newActivityType));
								
								counter.incCounter();
								
								// check capacity and remove facility from quad tree if the capacity limit is reached
								checkCapacity(activity.getFacilityId(), activity.getType());								
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
			
			/*
			 * If the person's plan has been adapted, there will be an entry in the map. If we
			 * find an entry, we have to recreate the person's desires and knowledge. 
			 */
//			if (relocationMapping.size() > 0) recreateKnowledgeAndDesires(person);
		}
		counter.printCounter();
		log.info("done.");
		
		/*
		 * We have to recreate the knowledge and desires for every person because sometimes their
		 * entries seem not to correspond with the person's plan.
		 */
		log.info("recreate knowledge and desires for population...");
		for (Person person : population.getPersons().values()) {
			recreateKnowledgeAndDesires(person);
		}
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
	private void relocateActivityTo(Activity activity, Id relocatedFacilityId, String activityType) {

		Facility relocatedFacility = zurichFacilities.getFacilities().get(relocatedFacilityId); 
		
		activity.setType(activityType);
		((ActivityImpl) activity).setFacilityId(relocatedFacilityId);
		((ActivityImpl) activity).setLinkId(relocatedFacility.getLinkId());
		((ActivityImpl) activity).setCoord(relocatedFacility.getCoord());
	}
	
	private void recreateKnowledgeAndDesires(Person person) {
		
		Desires desires = ((PersonImpl) person).getDesires();
		KnowledgeImpl knowledge = knowledges.getKnowledgesByPersonId().get(person.getId());
		
		for (String activityType : activityTypes) {
			if (desires.getActivityDurations() == null) break;
			else desires.removeActivityDuration(activityType);
		}
		Set<String> primaryActivityTypes = knowledge.getActivityTypes(true);
		knowledge.removeAllActivities();
		
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				
				/*
				 * Try to find facility in Zurich facilities. It is not contained, try finding it in the
				 * Switzerland facilities. If we cannot find it there, we prompt an error message.
				 */
				ActivityFacility facility = zurichFacilities.getFacilities().get(activity.getFacilityId());
				if (facility == null) facility = switzerlandFacilities.getFacilities().get(activity.getFacilityId());
				if (facility == null) log.error("Could not find facility with id: " + activity.getFacilityId());
				if (facility.getActivityOptions() == null) log.error("Could not find any activity option for facility with id: " + activity.getFacilityId());
				
				String activityType = activity.getType();
				desires.accumulateActivityDuration(activityType, activity.getEndTime() - activity.getStartTime());
				
				boolean isPrimary = primaryActivityTypes.contains(activityType);
				
				ActivityOption activityOption = facility.getActivityOptions().get(activityType);
				knowledge.addActivityOption(activityOption, isPrimary);
			}
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
