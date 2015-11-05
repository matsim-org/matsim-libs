/* *********************************************************************** *
 * project: org.matsim.*
 * LCPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.facilities.ActivityFacility;

/**
 * Optimized data structure used in the location choice contribution. There, a huge amount of plans are created and
 * evaluated, which is time consuming and increases the workload for the garbage collector.  
 * 
 * This class uses an optimized data structure designed especially for that application. There is no guarantee that
 * it can be utilized for other applications!
 * 
 * @author cdobler
 */
public class LCPlan implements Plan {

	private Double score = null;
	private Person person = null;
	private String type = null;
	
	private final List<PlanElement> planElements = new ArrayList<>();
	
	// Activity related arrays
	/*package*/ double[] startTimes;
	/*package*/ double[] endTimes;
	/*package*/ double[] durations;
	/*package*/ String[] types;
	/*package*/ Coord[] coords;
	/*package*/ Id<Link>[] linkIds;
	/*package*/ Id<ActivityFacility>[] facilityIds;
	
	// Leg related arrays
	/*package*/ Route[] routes;
	/*package*/ double[] depTimes;
	/*package*/ double[] arrTimes;
	/*package*/ double[] travTimes;
	/*package*/ String[] modes;
	
	public LCPlan(Plan plan) {
		copyFrom(plan, this);
	}
	
	@Override
	public final Map<String, Object> getCustomAttributes() {
		return null;
	}

	@Override
	public final void setScore(Double score) {
		this.score = score;
	}

	@Override
	public final Double getScore() {
		return this.score;
	}

	@Override
	public final boolean isSelected() {
		return this.getPerson().getSelectedPlan() == this;
	}

	@Override
	public final List<PlanElement> getPlanElements() {
		return this.planElements;
	}

	@Override
	public final void addLeg(Leg leg) {
		throw new RuntimeException("Not supported. Aborting!");
	}

	@Override
	public final void addActivity(Activity act) {
		throw new RuntimeException("Not supported. Aborting!");
	}

	@Override
	public final String getType() {
		return this.type;
	}

	@Override
	public final void setType(String type) {
		this.type = type;
	}

	@Override
	public final Person getPerson() {
		return this.person;
	}

	@Override
	public final void setPerson(Person person) {
		this.person = person;
	}
	
	//-------------------------------------------------------------------------
	// static methods
	//-------------------------------------------------------------------------
	
	public static LCLeg getPreviousLeg(LCPlan plan, LCActivity activity) {
		return (LCLeg) plan.getPlanElements().get(activity.getPlanElementIndex() - 1);
	}
	
	public static LCActivity getPreviousActivity(LCPlan plan, LCLeg leg) {
		return (LCActivity) plan.getPlanElements().get(leg.getPlanElementIndex() - 1);
	}
	
	public static LCLeg getNextLeg(LCPlan plan, LCActivity activity) {
		return (LCLeg) plan.getPlanElements().get(activity.getPlanElementIndex() + 1);
	}
	
	public static LCActivity getNextActivity(LCPlan plan, LCLeg leg) {
		return (LCActivity) plan.getPlanElements().get(leg.getPlanElementIndex() + 1);
	}
	
	public static LCPlan createCopy(LCPlan plan) {
		return new LCPlan(plan);
	}
	
	@SuppressWarnings("unchecked")
	public static void copyFrom(PlanImpl srcPlan, LCPlan destPlan) {
		
		int activityCount = 0;
		int legCount = 0;
		for (PlanElement planElement : srcPlan.getPlanElements()) {
			if (planElement instanceof Activity) activityCount++;
			else if (planElement instanceof Leg) legCount++;
			else throw new RuntimeException("Found unexpected PlanElement type: " + planElement.getClass().toString() + ". Aborting!");
		}
		
//		destPlan.activities = new LCActivity[activityCount];
		destPlan.startTimes = new double[activityCount];
		destPlan.endTimes = new double[activityCount];
		destPlan.durations = new double[activityCount];
		destPlan.types = new String[activityCount];
		destPlan.coords = new Coord[activityCount];
		destPlan.linkIds = new Id[activityCount];
		destPlan.facilityIds = new Id[activityCount];
				
//		destPlan.legs = new LCLeg[legCount];
		destPlan.routes = new Route[legCount];
		destPlan.depTimes = new double[legCount];
		destPlan.arrTimes = new double[legCount];
		destPlan.travTimes = new double[legCount];
		destPlan.modes = new String[legCount];
		
		activityCount = 0;
		legCount = 0;
		int planElementCount = 0;
		for (PlanElement planElement : srcPlan.getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				destPlan.planElements.add(new LCActivity(destPlan, activityCount, planElementCount));
				destPlan.startTimes[activityCount] = activity.getStartTime();
				destPlan.endTimes[activityCount] = activity.getEndTime();
				destPlan.durations[activityCount] = activity.getMaximumDuration();
				destPlan.types[activityCount] = activity.getType();
				destPlan.coords[activityCount] = activity.getCoord();
				destPlan.linkIds[activityCount] = activity.getLinkId();
				destPlan.facilityIds[activityCount] = activity.getFacilityId();
				activityCount++;
			}
			else if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				destPlan.planElements.add(new LCLeg(destPlan, legCount, planElementCount));
				destPlan.routes[legCount] = leg.getRoute();
				destPlan.depTimes[legCount] = leg.getDepartureTime();
				destPlan.arrTimes[legCount] = ((LegImpl) leg).getArrivalTime();
				destPlan.travTimes[legCount] = leg.getTravelTime();
				destPlan.modes[legCount] = leg.getMode();
				legCount++;
			}
			else throw new RuntimeException("Found unexpected PlanElement type: " + planElement.getClass().toString() + ". Aborting!");
			
			planElementCount++;
		}
		
		destPlan.type = srcPlan.getType();
		destPlan.person = srcPlan.getPerson();
		destPlan.score = srcPlan.getScore();
	}
	
	public static void copyFrom(Plan srcPlan, LCPlan destPlan) {
		if (srcPlan instanceof PlanImpl) copyFrom((PlanImpl) srcPlan, destPlan);
		else if (srcPlan instanceof LCPlan) copyFrom((LCPlan) srcPlan, destPlan);
		else throw new RuntimeException("Found unexpected source plan type: " + srcPlan.getClass().toString() + ". Aborting!");
	}
	
	@SuppressWarnings("unchecked")
	public static void copyFrom(LCPlan srcPlan, LCPlan destPlan) {

		// activity data
		int activities = srcPlan.startTimes.length;
		destPlan.startTimes = new double[activities];
		destPlan.endTimes = new double[activities];
		destPlan.durations = new double[activities];
		destPlan.types = new String[activities];
		destPlan.coords = new Coord[activities];
		destPlan.linkIds = new Id[activities];
		destPlan.facilityIds = new Id[activities];
		
		System.arraycopy(srcPlan.startTimes, 0, destPlan.startTimes, 0, activities);
		System.arraycopy(srcPlan.endTimes, 0, destPlan.endTimes, 0, activities);
		System.arraycopy(srcPlan.durations, 0, destPlan.durations, 0, activities);
		System.arraycopy(srcPlan.types, 0, destPlan.types, 0, activities);
		System.arraycopy(srcPlan.coords, 0, destPlan.coords, 0, activities);
		System.arraycopy(srcPlan.linkIds, 0, destPlan.linkIds, 0, activities);
		System.arraycopy(srcPlan.facilityIds, 0, destPlan.facilityIds, 0, activities);
		
		// leg data
		int legs = srcPlan.routes.length;
		destPlan.routes = new Route[legs];
		destPlan.depTimes = new double[legs];
		destPlan.arrTimes = new double[legs];
		destPlan.travTimes = new double[legs];
		destPlan.modes = new String[legs];
		
		System.arraycopy(srcPlan.routes, 0, destPlan.routes, 0, legs);
		System.arraycopy(srcPlan.depTimes, 0, destPlan.depTimes, 0, legs);
		System.arraycopy(srcPlan.arrTimes, 0, destPlan.arrTimes, 0, legs);
		System.arraycopy(srcPlan.travTimes, 0, destPlan.travTimes, 0, legs);
		System.arraycopy(srcPlan.modes, 0, destPlan.modes, 0, legs);
		
		int activityCount = 0;
		int legCount = 0;
		int planElementCount = 0;
		for (PlanElement planElement : srcPlan.getPlanElements()) {
			if (planElement instanceof Activity) {
				destPlan.planElements.add(new LCActivity(destPlan, activityCount, planElementCount));
				activityCount++;
			}
			else if (planElement instanceof Leg) {
				destPlan.planElements.add(new LCLeg(destPlan, legCount, planElementCount));
				legCount++;
			}
			else throw new RuntimeException("Found unexpected PlanElement type: " + planElement.getClass().toString() + ". Aborting!");
			
			planElementCount++;
		}
		
		// plan data
		destPlan.type = srcPlan.getType();
		destPlan.person = srcPlan.getPerson();
		destPlan.score = srcPlan.getScore();
	}
}