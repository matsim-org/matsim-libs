/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P.replan;

import java.util.Comparator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;

public class SchedulePlans {
	private final static Logger log = Logger.getLogger(SchedulePlans.class);
	
	private final int numberOfPlans;
	private final Id agentId;
	
	LinkedList<Tuple<TransitLine, Double>> plans; 
	
	public SchedulePlans(Id agentId, int numberOfPlans){
		this.numberOfPlans = numberOfPlans;
		this.agentId = agentId;
		this.plans = new LinkedList<Tuple<TransitLine, Double>>();
	}
	
	class CompareTransitPlans<T> implements Comparator<Tuple<TransitLine, Double>>{
		@Override
		public int compare(Tuple<TransitLine, Double> line1, Tuple<TransitLine, Double> line2) {
			return line1.getSecond().compareTo(line2.getSecond());
		}		
	}

	public void addTransitPlan(TransitLine line, Double score) {
		if(line.getId().toString().equalsIgnoreCase(this.agentId.toString())){
			if(this.plans.size() == this.numberOfPlans){
				removeWorstPlan();				
			}
			this.plans.add(new Tuple<TransitLine, Double>(line, score));
			
		} else {
			log.info("Wrong id provided. Expected " + this.agentId + ", but got " + line.getId());
		}
		
	}
	
	private void removeWorstPlan(){
		this.plans.removeFirst();
	}
	
	public Id getAgentId(){
		return this.agentId;
	}

	public TransitLine getBestPlan() {
		if(this.plans.size() == this.numberOfPlans){
			return this.plans.getLast().getFirst();
		} else {
			return null;
		}		
	}

}
