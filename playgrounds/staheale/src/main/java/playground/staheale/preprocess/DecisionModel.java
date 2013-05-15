/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionModel.java
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

package playground.staheale.preprocess;

import java.util.List;
import java.util.TreeMap;
import org.matsim.api.core.v01.population.Plan;

public class DecisionModel {
	
	private TreeMap<String, Double> activityFrequenciesPerDayWeek = new TreeMap<String, Double>();
	private TreeMap<String, Double> activityFrequenciesSat = new TreeMap<String, Double>();
	private TreeMap<String, Double> activityFrequenciesSun = new TreeMap<String, Double>();
	private AgentMemory memory;
		
	public void setMemory(AgentMemory memory) {
		this.memory = memory;
	}
		
	public boolean doesAct(String type, String day) {
		int numberOfActsDone = this.memory.getNumberOfActivities(type, day);
		double numberOfActsPlanned = 0.0;
		
		if (day.equals("sat")) {
			numberOfActsPlanned = this.activityFrequenciesSat.get(type);
		}
		else if (day.equals("sun")) {
			numberOfActsPlanned = this.activityFrequenciesSun.get(type);
		}
		else {
			numberOfActsPlanned = this.activityFrequenciesPerDayWeek.get(type) * 5.0;
		}	
		if (numberOfActsDone >= numberOfActsPlanned) {
			return false;
		}
		else {
			return true;
		}
	}
	
	public void setFrequency(String type, String day, double frequency) {
		if (day.equals("sat")) {
			this.activityFrequenciesSat.put(type, frequency);
		}
		else if (day.equals("sun")) {
			this.activityFrequenciesSun.put(type, frequency);
		}
		else {
			this.activityFrequenciesPerDayWeek.put(type, frequency);
		}
	}
		
	public Plan getPlan(List<Plan> plans, AgentMemory memory) {
		return plans.get(0);
	}
}

