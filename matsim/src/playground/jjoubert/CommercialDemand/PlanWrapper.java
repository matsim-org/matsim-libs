/* *********************************************************************** *
 * project: org.matsim.*
 * PlanWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.CommercialDemand;

import java.util.ArrayList;

import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulationBuilder;

public class PlanWrapper {
	private final BasicPopulationBuilder pb; 	// Population builder 
	private final int tw;						// Time window
	
	public PlanWrapper(BasicPopulationBuilder populationBuilder, int timeWindow){
		this.pb = populationBuilder;	
		this.tw = timeWindow;
	}

	/**
	 * The method receives an activity plan, which may or may not span a given time window, and 
	 * wrap it into separate plans, each fitting within the time window.
	 * 
	 * @param plan of type {@code BasicPlan}
	 * @param pb a population builder of type {@code BasicPopulationBuilder}
	 * @return an {@code ArrayList} of {@code BasicPlan}s
	 */
	@SuppressWarnings("unchecked")
	
	// TODO These things must be done:
	// - Generalize 24h
	// - Add private variable to indicate what activity type must be used to 'split' the chains (here "major")
	// - Generalize split activity type
	// - Plan sequence will not always be activity-leg-activity-leg-activity-leg... 
	// - Maybe, since activity types may be multiple, I should rather use the activity types in the chain to assign to the dummy activities. 
	public ArrayList<BasicPlan> wrapPlan(BasicPlan plan) {
		
		ArrayList<BasicPlan> result = new ArrayList<BasicPlan>();
		Object lastActivity = plan.getPlanElements().get(plan.getPlanElements().size() - 1);
		
		// I shouldn't have to test if the lastActivitiy is an instance of BasicActivity
		if( ((BasicActivity) lastActivity).getStartTime() > this.tw ){
			// Add the first activity
			BasicPlan dummyPlan = this.getPb().createPlan(null);
			BasicActivity firstActivity = (BasicActivity) plan.getPlanElements().get(0);
			dummyPlan.getPlanElements().add(firstActivity);
			BasicLeg firstLeg = (BasicLeg) plan.getPlanElements().get(1);
			dummyPlan.addLeg(firstLeg);
			
			int index = 2; 
			// Only have to check the activities... nope must change!
			while(index < plan.getPlanElements().size()){
				BasicActivity ba = (BasicActivity) plan.getPlanElements().get(index);
				if(ba.getType() == "minor"){ //TODO This must be generalized to ANY activity type
					if(ba.getEndTime() > 86400){
						// Create a new dummy activity, and add to end of current plan
						BasicActivity baDummy1 = pb.createActivityFromCoord("major", ba.getCoord() );
						baDummy1.setStartTime(86399); // 23:59:59
						dummyPlan.getPlanElements().add(baDummy1);
						result.add(dummyPlan);
						
						// Create a new dummy plan, and add the dummy activity as the first activity
						dummyPlan = pb.createPlan(null);
						BasicActivity baDummy2 = pb.createActivityFromCoord("major", ba.getCoord() );
						// Make it the start time of the first activity of the new day. This is fine since
						// it already has the same location as the first activity of the new day.
						baDummy2.setEndTime(1); // 00:00:01
						dummyPlan.getPlanElements().add(baDummy2);
						BasicLeg leg = (BasicLeg) plan.getPlanElements().get(index - 1);
						dummyPlan.getPlanElements().add(leg);
						
						// Add the remaining activities, adjusting the times
						while(index < plan.getPlanElements().size()){
							BasicActivity ba3 = (BasicActivity) plan.getPlanElements().get(index);
							if(ba3.getStartTime() > 0){
								double st = ba3.getStartTime();
								ba3.setStartTime(st - 86400);
							}
							if(ba3.getEndTime() > 0){
								double st = ba3.getEndTime();
								ba3.setEndTime(st - 86400);
							}
							dummyPlan.getPlanElements().add(ba3);
							if(ba3.getType() == "minor"){
								BasicLeg bl = (BasicLeg) plan.getPlanElements().get(index + 1);
								dummyPlan.getPlanElements().add(bl);
							}
							index += 2;
						}
						// Recursively check the new dummy plan
						PlanWrapper pb = new PlanWrapper(this.getPb(), this.getTw() );
						ArrayList<BasicPlan> recursivePlans = pb.wrapPlan(dummyPlan);
						for (BasicPlan bp : recursivePlans) {
							result.add(bp);
						}
						
					} else{
						dummyPlan.getPlanElements().add(ba);
						BasicLeg leg = (BasicLeg) plan.getPlanElements().get(index+1);
						dummyPlan.getPlanElements().add(leg);
						index += 2;
					}			
				} else {
					dummyPlan.getPlanElements().add(ba);
					result.add(dummyPlan);
					dummyPlan = pb.createPlan(null);
					index += 2;
				}
			}

		} else{
			// The plan only spans one day.
			result.add(plan);
		}
		return result;
	}
	
	
	public BasicPopulationBuilder getPb() {
		return pb;
	}

	
	public Integer getTw() {
		return tw;
	}

}
