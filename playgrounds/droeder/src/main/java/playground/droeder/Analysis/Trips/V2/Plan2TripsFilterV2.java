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
	private List<Id> unprocessedAgents = new ArrayList<Id>();
	
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
			this.unprocessedAgents.add(p.getId());
			return false;
		}
		
		for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
			//a leg can't be the last part of a plan
			if(pe instanceof Leg){
				temp.add(pe);
			}else if(pe instanceof Activity){
				//a Trip never ends with a pt interaction
				if(((Activity) pe).getType().equals("pt interaction")){
					temp.add(pe);
				}
				// if a plan consists of more than one element there will be a leg after an activity
				else if(first){
					temp.add(pe);
					first = false;
				}
				// an activity which is not the first and not a pt interaction is the last activity of a trip
				else{
					temp.add(pe);
					trip = new AnalysisTripV2();
					
					//store trips in chronological order
					trip.addElements((ArrayList<PlanElement>) temp);
					list.add(trip);
					
					// the following Trip starts with the same activity. If it was the last activity, temp will not be stored
					temp = new ArrayList<PlanElement>();
					temp.add(pe);
				}
			}
		}
		//add all Trips for this Person
		this.id2Trips.put(p.getId(), (LinkedList<AnalysisTripV2>) list);
		return true;
	}
	
	/**
	 * return a <code>String</code> of Id's of all Agents which are not processed, because they have only one activity
	 * @return
	 */
	public String getUnprocessedAgents(){
		StringBuffer b = new StringBuffer();
		b.append("following Agents are not processed, because their plan consists only of one Element\n");
		for(Id id : this.unprocessedAgents){
			b.append(id.toString() + "\n");
		}
		return b.toString();
	}
	
	
	/**
	 * returns a list of all Trips generated from PlansFile, sorted by AgentId
	 * @return
	 */
	public Map<Id, LinkedList<AnalysisTripV2>> getTrips(){
		if(this.notProcessed > 0){
			log.warn(this.notProcessed +" agents not processed, because they have a plan consisting of one or less elements! call getUnproccessedAgents() to get their Id's!");
		}
		return this.id2Trips;
	}
}
