/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsAssigner.java
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



import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.scoring.PlanScorer;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.population.Act;

import java.util.ArrayList;
import java.util.LinkedList;


/**
 * @author Matthias Feil
 * Parallel PlanAlgorithm to assign the non-optimized agents to an optimized agent
 * (= non-optimized agent copies the plan of the most similar optimized agent).
 */

public class AgentsAssigner1 extends AgentsAssigner implements PlanAlgorithm{ 
	
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	private final DistanceCoefficients coefficients;
	
	public AgentsAssigner1 (Controler controler, PreProcessLandmarks preProcessRoutingData, 
			LocationMutatorwChoiceSet locator, PlanScorer scorer, ScheduleCleaner cleaner, RecyclingModule recyclingModule,
			double minimumTime, DistanceCoefficients coefficients, LinkedList<String> nonassignedAgents){
		
		super(controler, preProcessRoutingData, locator, scorer,
				cleaner, recyclingModule, minimumTime, nonassignedAgents);
		this.coefficients = coefficients;
	}
	
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	public void run (Plan plan){
		
		OptimizedAgents agents = this.module.getOptimizedAgents();
		
		double distance = Double.MAX_VALUE;
		double distanceAgent;
		int assignedAgent = -1;
		
		optimizedAgentsLoop:
		for (int j=0;j<agents.getNumberOfAgents();j++){

			// hard constraints
			for (int i=0;i<plan.getPerson().getKnowledge().getActivities(true).size();i++){
				boolean in = false;
				for (int x=0;x<agents.getAgentPlan(j).getActsLegs().size()-2;x+=2){
					if (((Act)(agents.getAgentPlan(j).getActsLegs().get(x))).getType().equals(plan.getPerson().getKnowledge().getActivities(true).get(i).getType())){
						in = true;
						break;
					}
				}
				if (!in) {
					//log.warn("Anschlag optimizedAgentsLoop! Person "+plan.getPerson().getId()+" bei OptimizedAgent "+agents.getAgentPerson(j).getId());
					continue optimizedAgentsLoop;
				}
			}
			
			// distance (=soft) fitness
			distanceAgent=0;
			if (this.distance=="distance"){
				distanceAgent += this.coefficients.getPrimActsDistance()*plan.getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(plan.getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
			}
	
			if (this.homeLocation=="homelocation"){
			
				double homelocationAgentX = plan.getPerson().getKnowledge().getActivities("home", true).get(0).getFacility().getCenter().getX();
				double homelocationAgentY = plan.getPerson().getKnowledge().getActivities("home", true).get(0).getFacility().getCenter().getY();
			
				distanceAgent += this.coefficients.gethomeLocationDistance()*java.lang.Math.sqrt(java.lang.Math.pow((agents.getAgentPerson(j).getKnowledge().getActivities("home", true).get(0).getFacility().getCenter().getX()-homelocationAgentX),2)+
						java.lang.Math.pow((agents.getAgentPerson(j).getKnowledge().getActivities("home", true).get(0).getFacility().getCenter().getY()-homelocationAgentY),2));
			}
			if (distanceAgent<distance){
				/*if (Statistics.prt==true){
					if (agents.filling[j]>0){
						assignedAgent=j;
						distance = distanceAgent;
						agents.filling[j]--;
					}
				}
				else {*/
					assignedAgent=j;
					distance = distanceAgent;
				//}
			}
		}
		if (distance==Double.MAX_VALUE){
			log.warn("No agent to assign from found!");
			this.nonassignedAgents.add(plan.getPerson().getId().toString());
			return;
		}
		this.writePlan(agents.getAgentPlan(assignedAgent), plan);
		this.locator.handlePlan(plan);
		this.router.run(plan);
		this.timer.run(plan);
		
		if (Statistics.prt==true) {
			ArrayList<String> prt = new ArrayList<String>();
			prt.add(""+plan.getPerson().getId().toString());
			prt.add(""+agents.getAgentPerson(assignedAgent).getId().toString());
			prt.add(""+plan.getScore());
			for (int y=0;y<plan.getActsLegs().size();y+=2){
				prt.add(((Act)(plan.getActsLegs().get(y))).getType());
			}
			Statistics.list.add(prt);	
		}	
	}	
}
	
