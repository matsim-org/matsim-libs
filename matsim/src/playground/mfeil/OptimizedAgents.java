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
import org.matsim.population.Person;

/**
 * @author Matthias Feil
 * This class provides access to all agents that have been optimized during an iteration.
 * Attributes (such as number or type of primary activities, distance between primary activities
 * and others) may be calculated and provided to other classes, mainly the AgentsAssigner.
 */

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
			double tmpDistance=0;
			if (this.list.get(i).getPerson().getKnowledge().getActivities(true).size()>1){
				for (int k=0;k<this.list.get(i).getPerson().getKnowledge().getActivities(true).size()-1;k++){
					tmpDistance+=this.list.get(i).getPerson().getKnowledge().getActivities(true).get(k).getLocation().getCenter().calcDistance(this.list.get(i).getPerson().getKnowledge().getActivities(true).get(k+1).getLocation().getCenter());
				}
				tmpDistance+=this.list.get(i).getPerson().getKnowledge().getActivities(true).get(this.list.get(i).getPerson().getKnowledge().getActivities(true).size()-1).getLocation().getCenter().calcDistance(this.list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter());
			}
			this.distancesTestAgents[i]=tmpDistance;
		}
	}
	
	public double getAgentDistance (int agent){
		return this.distancesTestAgents[agent];
	}
	
	public int getNumberOfAgents (){
		return this.list.size();
	}
	
	public Plan getAgentPlan (int agent){
		return this.list.get(agent);
	}
	
	public Person getAgentPerson (int agent){
		return this.list.get(agent).getPerson();
	}
}
