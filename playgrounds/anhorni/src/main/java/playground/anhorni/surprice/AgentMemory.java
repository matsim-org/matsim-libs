/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.surprice;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

import playground.anhorni.surprice.preprocess.Zone;

public class AgentMemory {
	
	private List<Plan> plansWeek = new Vector<Plan>();
	private List<Plan> plansSat = new Vector<Plan>();
	private List<Plan> plansSun = new Vector<Plan>();
	
	private TreeMap<String, Plan> plans = new TreeMap<String, Plan>();
	
	private TreeMap<String, Integer> activitiesWeek = new TreeMap<String, Integer>();
	private TreeMap<String, Integer> activitiesSat = new TreeMap<String, Integer>();
	private TreeMap<String, Integer> activitiesSun = new TreeMap<String, Integer>();	
	private Zone homeZone;
	
	private final static Logger log = Logger.getLogger(AgentMemory.class);
			
	public void setHomeZone(Zone homeZone) {
		this.homeZone = homeZone;
	}
	
	public Zone getHomeZone() {
		return this.homeZone;
	}
	
	public void addPlan(Plan plan, String day) {
		if (day.equals("sat")) {
			this.plansSat.add(plan);		
			this.countActs(plan, this.activitiesSat);
		}
		else if (day.equals("sun")) {
			this.plansSun.add(plan);		
			this.countActs(plan, this.activitiesSun);
		}
		else {
			this.plansWeek.add(plan);		
			this.countActs(plan, this.activitiesWeek);
		}
		
		this.plans.put(day, plan);
	}
	
	public boolean getLagPurpose(double departureTime, String purpose, String day, String mode) {
		boolean containsMode = false;		
		PlanImpl planPreviousDay = null;
			
		if (day.equals("sun")) {
			planPreviousDay = (PlanImpl)this.plansSat.get(0);
		} 
		else {
			planPreviousDay = (PlanImpl)this.plansWeek.get(Surprice.days.indexOf(day) - 1);
		}
				
		for (PlanElement pe : planPreviousDay.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe;
				
				if (act.getType().equals(purpose) && planPreviousDay.getFirstActivity() != act) {					
					Leg leg = planPreviousDay.getPreviousLeg(act);					
					String modePrev = "undefined";							
					
					if (leg != null) {
						modePrev = leg.getMode();
					}					
					if (mode.equals(modePrev)) {
						containsMode = true;
					}
				}
			}
		}
		return containsMode;
	}
	
	public boolean getLagTime(double departureTime, String type, String day, String mode) {
		boolean containsMode = false;		
		Plan planPreviousDay = null;
		if (day.equals("sun")) {
			planPreviousDay = this.plansSat.get(0);
		} 
		else {
			planPreviousDay = this.plansWeek.get(Surprice.days.indexOf(day) - 1);
		}		
		for (PlanElement pe : planPreviousDay.getPlanElements()) {
			if (pe instanceof Leg) {
				LegImpl leg = (LegImpl)pe;			
				String modePrev = leg.getMode();
				
				if (modePrev.equals(mode) && Math.abs(leg.getDepartureTime() - departureTime) < 3600.0) {
					containsMode = true;	
				}
			}
		}
		return containsMode;
	}
	
	public Plan getRandomPlanAndRemove(String day, Random random) {
		if (day.equals("sat")) {
			Plan plan = this.plansSat.get(0);	
			this.plansSat.clear();
			return plan;
		}
		else if (day.equals("sun")) {
			Plan plan = this.plansSun.get(0);	
			this.plansSun.clear();
			return plan;		
		}
		else {
			int index = random.nextInt(this.plansWeek.size());
			Plan plan = this.plansWeek.get(index);			
			this.plansWeek.remove(index);
			return plan;
		}
	}
	
	public int getNumberOfActivities(String type, String day) {
		if (day.equals("sat")) {
			if (this.activitiesSat.containsKey(type)) return this.activitiesSat.get(type);
			else return 0;
		}
		else if (day.equals("sun")) {
			if (this.activitiesSun.containsKey(type)) return this.activitiesSun.get(type);
			else return 0;			
		}
		else {
			if (this.activitiesWeek.containsKey(type)) return this.activitiesWeek.get(type);
			else return 0;
		}
	}
		
	public List<String> getModeOfLastTripsWithSamePurpose(String purpose, List<Plan> plans) {
		List<String> modes = new Vector<String>();
		
		for (Plan plan : plans) {
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					
					if (act.getType().equals(purpose)) {
						PlanImpl pl = (PlanImpl)plan;
						String mode = pl.getPreviousLeg(act).getMode();
						modes.add(mode);
					}
				}
			}			
		}		
		return modes;
	}
	
	public String getMainModePreviousDay(String day) {
		String mode = "";
		int [] modeCounts = {0, 0, 0, 0};
		
		int dayIndex = Surprice.days.indexOf(day);
		if (dayIndex > 0) { 
			dayIndex--; // previous day
		}
		else {
			return mode;
		}
		
		Plan plan = this.plans.get(Surprice.days.get(dayIndex));
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				LegImpl leg = (LegImpl)pe;
				String m = leg.getMode();
				modeCounts[Surprice.modes.indexOf(m)] += 1;
			}
		}
		int maxVal = 0;
		int maxIndex = 0;
		for (int i = 0; i < 4; i++) {
			if (modeCounts[i] > maxVal) {
				maxVal = modeCounts[i];
				maxIndex = i;
			}
		}
		mode = Surprice.modes.get(maxIndex);	// TODO: handle equal numbers
		return mode;
	}
	
	// act frequency -------------------------------------------------------------	
	private void countActs(Plan plan, TreeMap<String, Integer> activities) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe;
				String type = act.getType();
				int nActs = 0;
				if (activities.containsKey(type)) {
					nActs = activities.get(type);					
				}
				nActs += 1;
				activities.remove(type);
				activities.put(type, nActs);
			}
		}
	}
}
