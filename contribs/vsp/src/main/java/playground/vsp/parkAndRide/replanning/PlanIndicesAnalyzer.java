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
package playground.vsp.parkAndRide.replanning;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.vsp.parkAndRide.PRConstants;


/**
 * @author ikaddoura
 *
 */
public class PlanIndicesAnalyzer {
//	private static final Logger log = Logger.getLogger(PlanIndicesAnalyzer.class);

	private List<PlanElement> planElements = new ArrayList<PlanElement>();
	
	private boolean hasParkAndRide = false;
	private boolean hasHomeActivity = false;
	private boolean hasWorkActivity = false;

	private List<Integer> homeActs = new ArrayList<Integer>();
	private List<Integer> workActs = new ArrayList<Integer>();
	private List<Integer> allActs = new ArrayList<Integer>();
	private List<Integer> prActs = new ArrayList<Integer>();
	
	public PlanIndicesAnalyzer(Plan plan) {
		this.planElements = plan.getPlanElements();
	}
	
	public void setIndices(){
		
		this.allActs.clear();
		this.homeActs.clear();
		this.workActs.clear();
		this.prActs.clear();
		
		for (int i = 0; i < planElements.size(); i++) {
			PlanElement pe = planElements.get(i);
			
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				this.allActs.add(i);
				if (act.toString().contains(PRConstants.PARKANDRIDE_ACTIVITY_TYPE)){
					hasParkAndRide = true;
					this.prActs.add(i);
				} else if (act.toString().contains("home")){
					hasHomeActivity = true;
					this.homeActs.add(i);						
				} else if (act.toString().contains("work")){
					hasWorkActivity = true;
					this.workActs.add(i);
				}
			}
		}
	}

	public List<Integer> getHomeActs() {
		return homeActs;
	}

	public List<Integer> getWorkActs() {
		return workActs;
	}

	public List<Integer> getAllActs() {
		return allActs;
	}

	public List<Integer> getPrActs() {
		return prActs;
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

	public int getMinHomeAfterWork(int workIndex) {
		int minHomeAfterWork = 999999;
		for (Integer homeIndex : this.getHomeActs()){
			if (homeIndex > workIndex){
				if (homeIndex < minHomeAfterWork){
					minHomeAfterWork = homeIndex;
				}
			}
		}
		return minHomeAfterWork;
	}

	public int getMaxHomeBeforeWork(int workIndex) {
		int maxHomeBeforeWork = 0;
		for (Integer homeIndex : this.getHomeActs()){
			if (homeIndex < workIndex){
				if (homeIndex > maxHomeBeforeWork){
					maxHomeBeforeWork = homeIndex;
				}
			}
		}
		return maxHomeBeforeWork;
	}
	
}
