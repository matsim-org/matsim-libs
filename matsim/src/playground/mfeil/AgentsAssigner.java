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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;


/**
 * @author Matthias Feil
 * Parallel PlanAlgorithm to assign the non-optimized agents to an optimized agent
 * (= non-optimized agent copies the plan of the most similar optimized agent).
 */

public class AgentsAssigner implements PlanAlgorithm{ 
	protected final Controler					controler;
	protected final PlanAlgorithm				timer;
	protected final LocationMutatorwChoiceSet 	locator;
	protected final PlanScorer					scorer;
	protected final PlansCalcRoute router;
	protected final RecyclingModule				module;
	protected final ScheduleCleaner				cleaner;
	protected final double						minimumTime;
	protected LinkedList<String>				nonassignedAgents;
	protected static final Logger 				log = Logger.getLogger(AgentsAssigner.class);
		
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	
	
	public AgentsAssigner (Controler controler, DepartureDelayAverageCalculator tDepDelayCalc, LocationMutatorwChoiceSet locator, PlanScorer scorer, RecyclingModule recyclingModule,
			double minimumTime, LinkedList<String> nonassignedAgents){
		this.controler				= controler;
		this.router 				= new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.scorer					= scorer;
		LegTravelTimeEstimator legTravelTimeEstimator = controler.getConfig().planomat().getLegTravelTimeEstimator(
				controler.getTravelTimeCalculator(), 
				tDepDelayCalc, 
				this.router);
		this.timer					= new TimeModeChoicer1(controler, legTravelTimeEstimator, this.scorer);
		//this.timer					= new TimeOptimizer14(legTravelTimeEstimator, this.scorer);
		this.locator 				= locator;
		this.module					= recyclingModule;
		this.minimumTime			= minimumTime;
		this.nonassignedAgents		= nonassignedAgents;
		this.cleaner				= new ScheduleCleaner(legTravelTimeEstimator, this.minimumTime);
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	@Deprecated
	public void run (Plan plan){
		
		OptimizedAgents agents = this.module.getOptimizedAgents();
		
		double distance = Double.MAX_VALUE;
		double distanceAgent;
		int assignedAgent = -1;
		/*
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
		*/
		
		
		this.writePlan(agents.getAgentPlan(assignedAgent), plan);
		this.locator.handlePlan(plan);
		this.router.run(plan);
		this.timer.run(plan);
		
		ArrayList<String> prt = new ArrayList<String>();
		prt.add(""+plan.getPerson().getId().toString());
		prt.add(""+agents.getAgentPerson(assignedAgent).getId().toString());
		prt.add(""+plan.getScoreAsPrimitiveType());
		Statistics.list.add(prt);	
				
	}	
	
	protected void writePlan (Plan in, Plan out){
		Plan bestPlan = new org.matsim.core.population.PlanImpl (in.getPerson());
		bestPlan.copyPlan(in);
		List<PlanElement> al = (List<PlanElement>) out.getPlanElements();
		
		// NEW NEW NEW NEW NEW NEW NEW
		ArrayList<ActivityOption> primActs = new ArrayList<ActivityOption>(out.getPerson().getKnowledge().getActivities(true));
		
		// TODO Check what is better! Condition that home activity is always first and last activity in day plan
		for (int i=0;i<primActs.size();i++){
			//if (primActs.get(i).getType().equals(((Act)(bestPlan.getActsLegs().get(0))).getType())) primActs.remove(i);
			if (primActs.get(i).getType().toString().equals("home")) primActs.remove(i);
		}
		
		for (int i=2;i<bestPlan.getPlanElements().size()-2;i+=2){
			if (!primActs.isEmpty()){
				for (int j=0;j<primActs.size();j++){
					if (((Activity)(bestPlan.getPlanElements().get(i))).getType().equals(primActs.get(j).getType())){
						ActivityFacility fac = this.controler.getFacilities().getFacilities().get(primActs.get(j).getFacility().getId());
						((Activity)(bestPlan.getPlanElements().get(i))).setFacility(fac);
						// not only update of fac required but also coord and link; data inconsistencies otherwise
						((Activity)(bestPlan.getPlanElements().get(i))).setCoord(fac.getCoord());
						((Activity)(bestPlan.getPlanElements().get(i))).setLink(fac.getLink());
						primActs.remove(j);
						break;
					}
				}
			}
		}
		
		if(al.size()>bestPlan.getPlanElements().size()){ 
			int i;
			for (i = 2; i<bestPlan.getPlanElements().size()-2;i++){
				al.remove(i);
				al.add(i, bestPlan.getPlanElements().get(i));	
			}
			for (int j = i; j<al.size()-2;j=j+0){
				al.remove(j);
			}
		}
		else if(al.size()<bestPlan.getPlanElements().size()){
			int i;
			for (i = 2; i<al.size()-2;i++){
				al.remove(i);
				al.add(i, bestPlan.getPlanElements().get(i));	
			}
			for (int j = i; j<bestPlan.getPlanElements().size()-2;j++){			
				al.add(j, bestPlan.getPlanElements().get(j));
			}
		}
		else {
			for (int i = 2; i<al.size()-2;i++){
			al.remove(i);
			al.add(i, bestPlan.getPlanElements().get(i));	
			}
		}
	}
	
	@Deprecated
	private void cleanUpPlan (Plan plan){
		double move = this.cleaner.run(((Activity)(plan.getPlanElements().get(0))).getEndTime(), plan);
		int loops=1;
		while (move!=0.0){
			loops++;
			move = this.cleaner.run(java.lang.Math.max(((Activity)(plan.getPlanElements().get(0))).getEndTime()-move,0), plan);
			if (loops>3) {
				for (int i=2;i< plan.getPlanElements().size()-4;i+=2){
					((Activity)(plan.getPlanElements().get(i))).setDuration(this.minimumTime);
				}
				move = this.cleaner.run(this.minimumTime, plan);
				if (move!=0.0){
					throw new IllegalArgumentException("No valid plan possible for person "+plan.getPerson().getId());
				}
			}
		}
	}
}
	
