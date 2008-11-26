/* *********************************************************************** *
 * project: org.matsim.*
 * OptimizedAgents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mfeil;

import java.util.ArrayList;
import org.matsim.population.Plan;
import org.matsim.basic.v01.Id;;

public class OptimizedAgents {
	
	private ArrayList<Plan> list;
	double [] distancesTestAgents;
	
	public OptimizedAgents (ArrayList<Plan> list){
		this.list = list;
		this.run();
	}
	
	private void run (){
		this.distancesTestAgents = new double [this.list.size()];
		for (int i=0;i<this.distancesTestAgents.length;i++){
			this.distancesTestAgents[i] = this.list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(this.list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
		}
	}
	
	public Id getAgent (int agent){
		return this.list.get(agent).getPerson().getId();
	}
	
	public double getAgentDistance (int agent){
		return this.distancesTestAgents[agent];
	}
	
	public int getNumberOfAgents (){
		return this.list.size()-1;
	}
	
	public Plan getAgentPlan (int agent){
		return this.list.get(agent);
	}
}
