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



import java.util.ArrayList;
import org.matsim.controler.Controler;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.util.PreProcessLandmarks;


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
	
	public AgentsAssigner1 (Controler controler, PreProcessLandmarks preProcessRoutingData, LegTravelTimeEstimator legTravelTimeEstimator,
			LocationMutatorwChoiceSet locator, PlanAlgorithm timer, ScheduleCleaner cleaner, RecyclingModule recyclingModule,
			double minimumTime, DistanceCoefficients coefficients){
		
		super(controler, preProcessRoutingData, legTravelTimeEstimator, locator, timer,
				cleaner, recyclingModule, minimumTime);
		this.coefficients = coefficients;
	}
	
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	public void run (Plan plan){
		
		OptimizedAgents agents = this.module.getOptimizedAgents();
		
		double distance = Double.MAX_VALUE;
		double distanceAgent = 0;
		int assignedAgent = -1;
		
		for (int j=0;j<agents.getNumberOfAgents();j++){
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
				assignedAgent=j;
				distance = distanceAgent;
			}
		}
		
		
		
		this.writePlan(agents.getAgentPlan(assignedAgent), plan);
		this.locator.handlePlan(plan);
		this.router.run(plan);
		this.timer.run(plan);
		
	}	
	
	
}
	
