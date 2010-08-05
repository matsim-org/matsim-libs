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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.knowledges.Knowledges;

/**
 * @author Matthias Feil
 * This class provides access to all agents that have been optimized during an iteration.
 * Attributes (such as number or type of primary activities, distance between primary activities
 * and others) may be calculated and provided to other classes, mainly AgentsAssigner.
 */

public class OptimizedAgents {
	
	private ArrayList<Plan> list;
	private ArrayList<Double> distancesTestAgents;
	private Knowledges knowledges;
	
	public OptimizedAgents (ArrayList<Plan> list, Knowledges knowledges){
		this.list = list;
		this.knowledges = knowledges;
		this.run();
	}
	
	private void run (){
		this.distancesTestAgents = new ArrayList<Double>();
		for (int i=0;i<this.list.size();i++){
			double tmpDistance=0;
			if (this.knowledges.getKnowledgesByPersonId().get(this.list.get(i).getPerson().getId()).getActivities(true).size()>1){
				for (int k=0;k<this.knowledges.getKnowledgesByPersonId().get(this.list.get(i).getPerson().getId()).getActivities(true).size()-1;k++){
					tmpDistance+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(this.list.get(i).getPerson().getId()).getActivities(true).get(k).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(this.list.get(i).getPerson().getId()).getActivities(true).get(k+1).getLocation().getCoord());
				}
				tmpDistance+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(this.list.get(i).getPerson().getId()).getActivities(true).get(this.knowledges.getKnowledgesByPersonId().get(this.list.get(i).getPerson().getId()).getActivities(true).size()-1).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(this.list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord());
			}
			this.distancesTestAgents.add(tmpDistance);
		}
	}
	
	public double getAgentDistance (int agent){
		return this.distancesTestAgents.get(agent);
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
	
	public void addAgent (Plan plan){
		/* this.list.add(plan); */	// this is not necessary as there is a flat link to list[0] anyway.
		double tmpDistance=0;
		if (this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()>1){
			for (int k=0;k<this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()-1;k++){
				tmpDistance+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k+1).getLocation().getCoord());
			}
			tmpDistance+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()-1).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(0).getLocation().getCoord());
		}
		this.distancesTestAgents.add(tmpDistance);
	}
}
