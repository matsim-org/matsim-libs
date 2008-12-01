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
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;


/**
 * @author Matthias Feil
 * Parallel PlanAlgorithm to assign the non-optimized agents to an optimized agent
 * (= non-optimized agent copies the plan of the most similar optimized agent).
 */

public class AgentsAssigner implements PlanAlgorithm{ 
	protected final Controler					controler;
	protected final PlanAlgorithm				timer;
	protected final LocationMutatorwChoiceSet 	locator;
	protected final PlansCalcRouteLandmarks 	router;
	protected final RecyclingModule				module;
	protected final ScheduleCleaner				cleaner;
	protected final double						minimumTime;
	protected final String						distance, homeLocation;
	protected LinkedList<String>					nonassignedAgents;
	protected static final Logger 				log = Logger.getLogger(AgentsAssigner.class);
		
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	
	
	public AgentsAssigner (Controler controler, PreProcessLandmarks preProcessRoutingData, LegTravelTimeEstimator legTravelTimeEstimator,
			LocationMutatorwChoiceSet locator, PlanAlgorithm timer, ScheduleCleaner cleaner, RecyclingModule recyclingModule,
			double minimumTime, LinkedList<String> nonassignedAgents){
		this.controler				= controler;
		this.router 				= new PlansCalcRouteLandmarks (controler.getNetwork(), preProcessRoutingData, controler.getTravelCostCalculator(), controler.getTravelTimeCalculator());
		this.timer					= timer;
		this.locator 				= locator;
		this.cleaner				= cleaner;
		this.module					= recyclingModule;
		this.minimumTime			= minimumTime;
		this.distance				= "distance";
		this.homeLocation			= "homelocation";
		this.nonassignedAgents		= nonassignedAgents;
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	public void run (Plan plan){
		
		OptimizedAgents agents = this.module.getOptimizedAgents();
		
		double distance = Double.MAX_VALUE;
		double distanceAgent;
		int assignedAgent = -1;
		
		for (int j=0;j<agents.getNumberOfAgents();j++){
			distanceAgent=0;
			if (this.distance=="distance"){
				distanceAgent += plan.getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(plan.getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
			}
	
			if (this.homeLocation=="homelocation"){
			
				double homelocationAgentX = plan.getPerson().getKnowledge().getActivities("home", true).get(0).getFacility().getCenter().getX();
				double homelocationAgentY = plan.getPerson().getKnowledge().getActivities("home", true).get(0).getFacility().getCenter().getY();
			
				distanceAgent += java.lang.Math.sqrt(java.lang.Math.pow((agents.getAgentPerson(j).getKnowledge().getActivities("home", true).get(0).getFacility().getCenter().getX()-homelocationAgentX),2)+
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
		
		Statistics.list.add(new String []{plan.getPerson().getId().toString(),agents.getAgentPerson(assignedAgent).getId().toString(),""+plan.getScore()});	
				
	}	
	
	protected void writePlan (Plan in, Plan out){
		Plan bestPlan = new Plan (in.getPerson());
		bestPlan.copyPlan(in);
		ArrayList<Object> al = out.getActsLegs();
		
		// NEW NEW NEW NEW NEW NEW NEW
		ArrayList<Activity> primActs = new ArrayList<Activity>(out.getPerson().getKnowledge().getActivities(true));
		
		// Condition that home activity is always first and last activity in day plan
		for (int i=0;i<primActs.size();i++){
			if (primActs.get(i).getType().equals(((Act)(bestPlan.getActsLegs().get(0))).getType())) primActs.remove(i);
		}
		
		for (int i=2;i<bestPlan.getActsLegs().size()-2;i+=2){
			if (!primActs.isEmpty()){
				for (int j=0;j<primActs.size();j++){
					if (((Act)(bestPlan.getActsLegs().get(i))).getType().equals(primActs.get(j).getType())){
						Facility fac = (Facility) this.controler.getFacilities().getLocation(primActs.get(j).getFacility().getId());
						((Act)(bestPlan.getActsLegs().get(i))).setFacility(fac);
						//log.info("Für type "+primActs.get(j).getType()+", set Facility = "+primActs.get(j).getFacility().getId()+" für Person "+out.getPerson().getId());
						primActs.remove(j);
						break;
					}
				}
				//log.info("Für type "+((Act)(bestPlan.getActsLegs().get(i))).getType()+", Facility = "+((Act)(bestPlan.getActsLegs().get(i))).getFacility().getId()+" für Person "+out.getPerson().getId());
			}
		}
		
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
	
	@Deprecated
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
	
