/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.Trips.V2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.filters.AbstractPersonFilter;

/**
 * @author droeder
 *
 */
public class Plan2TripsFilterV2 extends AbstractPersonFilter {
	private static final Logger log = Logger.getLogger(Plan2TripsFilterV2.class);
	int notProcessed = 0;

	private Map<Id, LinkedList<AnalysisTripV2>> id2Trips = new HashMap<Id, LinkedList<AnalysisTripV2>>();
	
	@Override
	public void run(Person p){
		if(judge(p)){
			this.count();
		}
	}
	
	@Override
	public boolean judge(Person p) {
		List<AnalysisTripV2> list = new LinkedList<AnalysisTripV2>();
		List<PlanElement> temp = new ArrayList<PlanElement>();
		AnalysisTripV2 trip;
		boolean first = true;
		
		// use only Plans with more than one Element
		if(p.getSelectedPlan().getPlanElements().size()<2){
			this.notProcessed++;
			return false;
		}
		
		for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
			if(pe instanceof Leg){
				temp.add(pe);
			}else if(pe instanceof Activity){
				if(((Activity) pe).getType().equals("pt interaction")){
					temp.add(pe);
				}else if(first){
					temp.add(pe);
					first = false;
				}else{
					temp.add(pe);
					trip = new AnalysisTripV2();
					trip.addElements((ArrayList<PlanElement>) temp);
					list.add(trip);
					temp = new ArrayList<PlanElement>();
					temp.add(pe);
				}
			}
		}
		this.id2Trips.put(p.getId(), (LinkedList<AnalysisTripV2>) list);
		return true;
	}
	
	public Map<Id, LinkedList<AnalysisTripV2>> getTrips(){
		if(this.notProcessed > 0){
			log.warn(this.notProcessed +" agents not processed, because they have a plan consisting of one or less elements");
		}
		return this.id2Trips;
	}
}
