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

package playground.anhorni.surprice.preprocess;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;

public class PersonWeeks {	
	private Person person;
	private ArrayList<TreeMap<Integer, Plan>> days = new ArrayList<TreeMap<Integer, Plan>>();
	private int currentWeek = - 1;
	private boolean isWorker = false;
	private double pweight = 1.0;
	private double income = 0.0;
	
	private final static Logger log = Logger.getLogger(PersonWeeks.class);		
	
	public void increaseWeek() {
		this.currentWeek++;
	}
	
	public PersonWeeks(Person person) {
		this.person = person;
		
		for (int w = 0; w < 6; w++) {
			this.days.add(w, new TreeMap<Integer, Plan>());
		}
	}
	
	public Plan getDay(int dow, int week) {
		int w = week;
		while (w < 5 && this.days.get(w).size() < 7) { // week is not complete -> take next week
			w++;
		}
		return this.days.get(w).get(dow);
	}
	
	public void addDay(int dow, Plan plan) {
		if (this.currentWeek < 6) {
			this.days.get(this.currentWeek).put(dow, plan);
		}
	}
	
	public void removeIncompleteWeeks() {
		ArrayList<TreeMap<Integer, Plan>> removeWeeks = new ArrayList<TreeMap<Integer, Plan>>();
		for (int w = 0; w < days.size(); w++) {			
			if (this.days.get(w).size() < 7) {
				removeWeeks.add(this.days.get(w));
			}
		}
		this.days.removeAll(removeWeeks);
		log.info("removing incomplete weeks: " + this.days.size() + " weeks left for person " + this.person.getId());
	}
	
	public boolean hasCompleteWeek() {
		if (this.days.size() > 0) {
			return true;
		}
		else {
			return false;
		}
	}
		
	public void setWorkState() {
		for (int i = 0; i < days.size(); i++) {
			for (Plan plan : this.days.get(i).values()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						ActivityImpl act = (ActivityImpl)pe;				
						if (act.getType().startsWith("w")) {
							this.isWorker = true;
						}
					}
				}
			}
		}
	}
	public Person getPerson() {
		return this.person;
	}
	public boolean isWorker() {
		return isWorker;
	}
	public double getPweight() {
		return pweight;
	}
	public void setPweight(double pweight) {
		this.pweight = pweight;
	}
	public int getCurrentWeek() {
		return currentWeek;
	}
	public void setCurrentWeek(int currentWeek) {
		this.currentWeek = currentWeek;
	}

	public double getIncome() {
		return income;
	}

	public void setIncome(double income) {
		this.income = income;
	}
}
