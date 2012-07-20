/* *********************************************************************** *
 * project: org.matsim.*
 * PlanIndicesAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.pRstrategy;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideConstants;

/**
 * @author Ihab
 *
 */
public class PlanIndicesAnalyzer {
	private static final Logger log = Logger.getLogger(PlanIndicesAnalyzer.class);

	private List<PlanElement> planElements = new ArrayList<PlanElement>();
	
	private boolean hasParkAndRide = false;
	private boolean hasHomeActivity = false;
	private boolean hasWorkActivity = false;
	private boolean nextHomeIsFirstHomeAfterWork = false;
	
	private List<Integer> pRplanElementIndex = new ArrayList<Integer>();
	private List<Integer> actsplanElementIndex = new ArrayList<Integer>();
	private List<Integer> workPlanElementIndex = new ArrayList<Integer>();
	private List<Integer> homeBeforeWorkIndex = new ArrayList<Integer>();
	private List<Integer> homeAfterWorkIndex = new ArrayList<Integer>();

//	private int homeIndexBeforeWorkActivity = 0;
	private int homeIndexTmp = 0;
//	private int homeIndexAfterWorkActivity = 0;
	
	public PlanIndicesAnalyzer(Plan plan) {
		this.planElements = plan.getPlanElements();
	}

	public List<PlanElement> getPlanElements() {
		return planElements;
	}

	public boolean hasParkAndRide() {
		return hasParkAndRide;
	}

	public boolean hasHomeActivity() {
		return hasHomeActivity;
	}

	public boolean hasWorkActivity() {
		return hasWorkActivity;
	}

	public List<Integer> getpRplanElementIndex() {
		return pRplanElementIndex;
	}

	public List<Integer> getActsplanElementIndex() {
		return actsplanElementIndex;
	}

	public List<Integer> getWorkPlanElementIndex() {
		return workPlanElementIndex;
	}

	public int getHomeIndexBeforeWorkActivity() {
		return this.homeBeforeWorkIndex.get(0);
	}

	public int getHomeIndexAfterWorkActivity() {
		return this.homeAfterWorkIndex.get(this.homeAfterWorkIndex.size()-1);
	}
	
	public void setIndices(){
		
		this.actsplanElementIndex.clear();
		this.pRplanElementIndex.clear();
		this.workPlanElementIndex.clear();
		this.homeBeforeWorkIndex.clear();
		this.homeAfterWorkIndex.clear();
		
//		this.homeIndexAfterWorkActivity = 0;
//		this.homeIndexBeforeWorkActivity = 0;
		
		for (int i = 0; i < planElements.size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				actsplanElementIndex.add(i);
				if (act.toString().contains(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
					hasParkAndRide = true;
					pRplanElementIndex.add(i);
				} else if (act.toString().contains("home")){
					hasHomeActivity = true;
					homeIndexTmp = i;
					if (nextHomeIsFirstHomeAfterWork == true){
//						homeIndexAfterWorkActivity = i;
						homeAfterWorkIndex.add(i);
						nextHomeIsFirstHomeAfterWork = false;
					}						
				} else if (act.toString().contains("work")){
					homeBeforeWorkIndex.add(homeIndexTmp);
//					homeIndexBeforeWorkActivity = homeIndexTmp;
					hasWorkActivity = true;
					nextHomeIsFirstHomeAfterWork = true;
					workPlanElementIndex.add(i);
				}
			}
		}
		
		if (homeBeforeWorkIndex.size() > 1) {
			log.warn("Activity pattern created: Home, P+R, Work, Home, Work, P+R, Home");
		}
		
//		System.out.println("Indices set.");
	}

	public int getFirstWorkIndex() {
		return this.workPlanElementIndex.get(0);
	}

	public int getLastWorkIndex() {
		return this.workPlanElementIndex.get(workPlanElementIndex.size()-1);
	}
}
