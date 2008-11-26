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
 * Parallel PlanAlgorithm to assign the non-optimized agents to an optimized agent.
 */

public class AgentsAssigner implements PlanAlgorithm{ 
	
	private final PreProcessLandmarks		preProcessRoutingData;
	private final PlanAlgorithm				timer;
	private final LocationMutatorwChoiceSet locator;
	private final PlansCalcRouteLandmarks 	router;
	private final String					mode;
	private final RecyclingModule			module;
	private final ScheduleCleaner			cleaner;
	private final double					minimumTime;
	
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	
	
	public AgentsAssigner (Controler controler, PreProcessLandmarks preProcessRoutingData, LegTravelTimeEstimator legTravelTimeEstimator,
			LocationMutatorwChoiceSet locator, PlanAlgorithm timer, ScheduleCleaner cleaner, RecyclingModule recyclingModule,
			double minimumTime){
		this.preProcessRoutingData 	= preProcessRoutingData;
		this.router 				= new PlansCalcRouteLandmarks (controler.getNetwork(), this.preProcessRoutingData, controler.getTravelCostCalculator(), controler.getTravelTimeCalculator());
		this.timer					= timer;
		this.locator 				= locator;
		this.cleaner				= cleaner;
		this.module					= recyclingModule;
		this.mode					= "timer";	
		this.minimumTime			= minimumTime;
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	public void run (Plan plan){
		
		OptimizedAgents agents = this.module.getOptimizedAgents();
		
		double distance = Double.MAX_VALUE;
		int assignedAgent = -1;
		double distanceAgent = plan.getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(plan.getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
		for (int j=0;j<agents.getNumberOfAgents();j++){
			if (java.lang.Math.abs(distanceAgent-agents.getAgentDistance(j))<distance){
				assignedAgent=j;
				distance = java.lang.Math.abs(distanceAgent-agents.getAgentDistance(j));
			}
		}
		this.writePlan(agents.getAgentPlan(assignedAgent), plan);
		this.locator.handlePlan(plan);
		this.router.run(plan);
		if (this.mode.equals("timer")){
			this.timer.run(plan);
		}
		else this.cleanUpPlan(plan);
		
	}	
	
	private void writePlan (Plan in, Plan out){
		Plan bestPlan = new Plan (in.getPerson());
		bestPlan.copyPlan(in);
		ArrayList<Object> al = out.getActsLegs();
		if(al.size()>bestPlan.getActsLegs().size()){ 
			int i;
			for (i = 2; i<bestPlan.getActsLegs().size()-2;i++){
				al.remove(i);
				al.add(i, bestPlan.getActsLegs().get(i));	
			}
			for (int j = i; j<al.size()-2;j=j+0){
				al.remove(j);
			}
		}
		else if(al.size()<bestPlan.getActsLegs().size()){
			int i;
			for (i = 2; i<al.size()-2;i++){
				al.remove(i);
				al.add(i, bestPlan.getActsLegs().get(i));	
			}
			for (int j = i; j<bestPlan.getActsLegs().size()-2;j++){			
				al.add(j, bestPlan.getActsLegs().get(j));
			}
		}
		else {
			for (int i = 2; i<al.size()-2;i++){
			al.remove(i);
			al.add(i, bestPlan.getActsLegs().get(i));	
			}
		}
	}
	
	private void cleanUpPlan (Plan plan){
		double move = this.cleaner.run(((Act)(plan.getActsLegs().get(0))).getEndTime(), plan);
		int loops=1;
		while (move!=0.0){
			loops++;
			move = this.cleaner.run(java.lang.Math.max(((Act)(plan.getActsLegs().get(0))).getEndTime()-move,0), plan);
			if (loops>3) {
				for (int i=2;i< plan.getActsLegs().size()-4;i+=2){
					((Act)(plan.getActsLegs().get(i))).setDuration(this.minimumTime);
				}
				move = this.cleaner.run(this.minimumTime, plan);
				if (move!=0.0){
					throw new IllegalArgumentException("No valid plan possible for person "+plan.getPerson().getId());
				}
			}
		}
	}
}
	
