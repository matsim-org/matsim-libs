/* *********************************************************************** *
 * project: org.matsim.*
 * MyPlansProcessor.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import cern.colt.matrix.impl.DenseObjectMatrix2D;

public class MyPlansProcessor {
	private Logger log = Logger.getLogger(MyPlansProcessor.class);
	private Scenario scenario;
	private List<MyZone> zones;
	private Map<Id, Integer> zoneMap;
	private DenseObjectMatrix2D odMatrix;
	
	public MyPlansProcessor(Scenario scenario, List<MyZone> zones) {
		this.scenario = scenario;
		this.zones = zones;
		this.odMatrix = new DenseObjectMatrix2D(zones.size(), zones.size());
		zoneMap = new HashMap<Id, Integer>(zones.size());
		int index = 0;
		for(MyZone z : zones){
			zoneMap.put(z.getId(), index++);
		}
	}
	
	public void processPlans(){
		for(Person person : scenario.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			PlanElement pe;
			for(int i = 0; i < plan.getPlanElements().size(); i++){
				pe = plan.getPlanElements().get(i);
				if(pe instanceof Leg){
					Leg l = (Leg) pe;
					if(l.getMode().equals(TransportMode.car)){
//						// TODO find the origin node's zone;				
//						int o = getLastActivity(plan, i);
//						// TODO find the destination node's zone;
//						int d = getNextActivity(plan, i);
//						// Update travel
//						if(odMatrix.get(oIndex, dIndex) == null){
//							List<Double> list = new ArrayList<Double>();
//							list.add(l.getTravelTime());
//							odMatrix.set(oIndex, dIndex, list);
//						} else{
//							((List<Double>) odMatrix.get(oIndex, dIndex)).add(l.getTravelTime());
//						}
					}
				}
				
			}
		}
	}
	
	private Integer getLastActivity(Plan plan, int index){
		Integer result = null;
		do {
			if(plan.getPlanElements().get(index-1) instanceof Activity){
				result = index-1;
			} else{
				result = getLastActivity(plan, index-1);
			}
		} while (index >= 0 && result==null);
		if(index < 0){
			throw new RuntimeException("Plan does not start with an activity.");
		}
		return result;
	}
	
	private Integer getNextActivity(Plan plan, int index){
		Integer result = null;
		do {
			if(plan.getPlanElements().get(index+1) instanceof Activity){
				result = index+1;
			} else{
				result = getNextActivity(plan, index+1);
			}
		} while (index < plan.getPlanElements().size() && result == null);
		if(index >= plan.getPlanElements().size()){
			throw new RuntimeException("Plan does not end with an activity.");
		}
		return result;
	}

	public Scenario getScenario() {
		return scenario;
	}

	public DenseObjectMatrix2D getOdMatrix() {
		return odMatrix;
	}

	public List<MyZone> getZones() {
		return this.zones;
	}
}

