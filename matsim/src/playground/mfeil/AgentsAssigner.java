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
import org.matsim.core.api.ScenarioImpl;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;

import playground.mfeil.MDSAM.ActivityTypeFinder;


/**
 * @author Matthias Feil
 * Parallel PlanAlgorithm to assign the non-optimized agents to an optimized agent
 * (= non-optimized agent copies the plan of the most similar optimized agent).
 */

public class AgentsAssigner implements PlanAlgorithm{ 
	
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	protected final Controler					controler;
	protected final PlanAlgorithm 				timer;
	protected final LocationMutatorwChoiceSet 	locator;
	protected final RecyclingModule				module;
	protected LinkedList<String>				nonassignedAgents;
	protected Knowledges 						knowledges;
	private final ActivityTypeFinder 			finder;
	protected static final Logger 				log = Logger.getLogger(AgentsAssigner.class);
		
	
	private final DistanceCoefficients coefficients;
	private String primActsDistance, homeLocation, age, sex, license, car_avail, employed;
	
	
	public AgentsAssigner (Controler controler, DepartureDelayAverageCalculator 	tDepDelayCalc,
			LocationMutatorwChoiceSet locator, PlanScorer scorer, ActivityTypeFinder finder, RecyclingModule recyclingModule,
			DistanceCoefficients coefficients, LinkedList<String> nonassignedAgents){
		
		this.controler				= controler;
		PlansCalcRoute router 		= new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		LegTravelTimeEstimator legTravelTimeEstimator = new CetinCompatibleLegTravelTimeEstimator(
				controler.getTravelTimeCalculator(), 
				tDepDelayCalc, 
				router);
		this.timer					= new TimeModeChoicer2(controler, legTravelTimeEstimator, scorer);
		this.locator 				= locator;
		this.finder					= finder;
		this.module					= recyclingModule;
		this.nonassignedAgents		= nonassignedAgents;
		this.knowledges = ((ScenarioImpl)controler.getScenarioData()).getKnowledges();
		
		this.coefficients = coefficients;
		this.primActsDistance	="no";
		this.homeLocation		="no";
		this.age				="no";
		this.sex				="no";
		this.license			="no";
		this.car_avail			="no";
		this.employed			="no";
		for (int i=0; i<this.coefficients.getNamesOfCoef().size();i++){
			if (this.coefficients.getNamesOfCoef().get(i).equals("primActsDistance")) this.primActsDistance="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("homeLocation")) this.homeLocation="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("age")) this.age="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("sex")) this.sex="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("license")) this.license="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("car_avail")) this.car_avail="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("employed")) this.employed="yes";			
		}
		
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

			/* Hard constraints */
			
			// All prim acts in potential agent's plan
			for (int i=0;i<this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size();i++){
				boolean in = false;
				for (int x=0;x<agents.getAgentPlan(j).getPlanElements().size()-2;x=x+2){
					// try statement with print block to analyze some strange exceptions in Zurich scenario
					try {
					if (((Activity)(agents.getAgentPlan(j).getPlanElements().get(x))).getType().equals(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(i).getType())){
						in = true;
						break;
					}
					} catch (Exception e){
						log.warn(e);
						log.warn("Acts im Plan des schon optimierten Agenten:");
						for (int k=0;k<agents.getAgentPlan(j).getPlanElements().size();k++) {
							if (agents.getAgentPlan(j).getPlanElements().get(k).getClass().getName().equals("org.matsim.population.Act")) log.warn(((Activity)(agents.getAgentPlan(j).getPlanElements().get(k))).getType()+" ");
							else log.warn(((Leg)(agents.getAgentPlan(j).getPlanElements().get(k))).getMode()+" ");
						}
						System.out.println();
						log.warn("Primacts im Knowledge des zuzuordnenden Agenten:");
						for (int k=0;k< this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size();k++){
							if (this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k).getClass().getName().equals("org.matsim.population.Act")) log.warn(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k).getType()+" ");
							else log.warn("Leg but undefined. ");
						}
						System.out.println();
						continue optimizedAgentsLoop;	// if exception occurs go to next agent whatsoever the exception is.
					}
				}
				if (!in) {
					continue optimizedAgentsLoop;
				}
			}
			
			// All act types complying with those eligible to the agent (see ActivityTypeFinder)
			List<String> actTypes = this.finder.getActTypes(plan.getPerson());
			for (int i=2;i<plan.getPlanElements().size()-2;i+=2){ // "home" does not need to be checked
				if (!actTypes.contains(((Activity)(plan.getPlanElements().get(i))).getType())) {
					continue optimizedAgentsLoop;
				}
			}
			
			// Further hard constraints
				
			// Gender
			if (this.sex=="yes"){
				try{
					if (!plan.getPerson().getSex().equals(agents.getAgentPerson(j).getSex())) continue optimizedAgentsLoop;
				}
				catch (Exception e) {
					Statistics.noSexAssignment = true;
				}
			}
			
			// License
			if (this.license=="yes"){
				try{
					if (!plan.getPerson().getLicense().equals(agents.getAgentPerson(j).getLicense())) continue optimizedAgentsLoop;
				}
				catch (Exception e){
					Statistics.noLicenseAssignment = true;
				}
			}
			
			// Car availability
			if (this.car_avail=="yes"){
				try{
					if (!plan.getPerson().getCarAvail().equals(agents.getAgentPerson(j).getCarAvail())) continue optimizedAgentsLoop;
				}
				catch (Exception e){
					Statistics.noCarAvailAssignment = true;
				}
			}
			
			// Employment status
			if (this.employed=="yes"){
				try{
					if (!plan.getPerson().isEmployed().equals(agents.getAgentPerson(j).isEmployed())) continue optimizedAgentsLoop;
				}
				catch (Exception e){
					Statistics.noEmploymentAssignment = true;
				}
			}
						
			
			/* Distance (=soft) fitness */
			
			distanceAgent=0;
			// Distance between primary activities
			if (this.primActsDistance=="yes"){
				double tmpDistance=0;
				if (this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()>1){
					for (int k=0;k<this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()-1;k++){
						tmpDistance+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k+1).getLocation().getCoord());
					}
					tmpDistance+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()-1).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(0).getLocation().getCoord());
				}
				distanceAgent+=	this.coefficients.getSingleCoef("primActsDistance")*(java.lang.Math.abs(tmpDistance-this.module.getOptimizedAgents().getAgentDistance(j)));		
			}
			
			// Distance between home location of potential agent to copy from and home location of agent in question
			if (this.homeLocation=="yes"){			
				double homelocationAgentX = this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities("home", true).get(0).getFacility().getCoord().getX();
				double homelocationAgentY = this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities("home", true).get(0).getFacility().getCoord().getY();
			
				distanceAgent += this.coefficients.getSingleCoef("homeLocationDistance")*java.lang.Math.sqrt(java.lang.Math.pow((this.knowledges.getKnowledgesByPersonId().get(agents.getAgentPerson(j).getId()).getActivities("home", true).get(0).getFacility().getCoord().getX()-homelocationAgentX),2)+
						java.lang.Math.pow((this.knowledges.getKnowledgesByPersonId().get(agents.getAgentPerson(j).getId()).getActivities("home", true).get(0).getFacility().getCoord().getY()-homelocationAgentY),2));
			}
			
			// TODO @mfeil: exception handling missing
			if (this.age=="yes"){
				distanceAgent+= this.coefficients.getSingleCoef("age")* (plan.getPerson().getAge()-agents.getAgentPerson(j).getAge());
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
		this.timer.run(plan);  // includes to write the new score to the plan
		
		if (Statistics.prt==true) {
			ArrayList<String> prt = new ArrayList<String>();
			prt.add(plan.getPerson().getId().toString());
			prt.add(agents.getAgentPerson(assignedAgent).getId().toString());
			prt.add(plan.getScore().toString());
			for (int y=0;y<plan.getPlanElements().size();y+=2){
				prt.add(((Activity)(plan.getPlanElements().get(y))).getType());
			}
			Statistics.list.add(prt);	
		}	
	}
	
	
	protected void writePlan (Plan in, Plan out){
		Plan bestPlan = new org.matsim.core.population.PlanImpl (in.getPerson());
		bestPlan.copyPlan(in);
		List<PlanElement> al = (List<PlanElement>) out.getPlanElements();
		
		// NEW NEW NEW NEW NEW NEW NEW
		ArrayList<ActivityOption> primActs = new ArrayList<ActivityOption>(this.knowledges.getKnowledgesByPersonId().get(out.getPerson().getId()).getActivities(true));
		
		for (int i=2;i<bestPlan.getPlanElements().size()-2;i+=2){
			if (!primActs.isEmpty()){
				for (int j=0;j<primActs.size();j++){
					if (((Activity)(bestPlan.getPlanElements().get(i))).getType().equals(primActs.get(j).getType())){
						ActivityFacility fac = this.controler.getFacilities().getFacilities().get(primActs.get(j).getFacility().getId());
						((Activity)(bestPlan.getPlanElements().get(i))).setFacility(fac);
						// not only update of fac required but also coord and link; data inconsistencies otherwise
						((Activity)(bestPlan.getPlanElements().get(i))).setCoord(fac.getCoord());
						((Activity)(bestPlan.getPlanElements().get(i))).setLink(fac.getLink());
						if (!primActs.get(j).getType().toString().equals("home")) primActs.remove(j);
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
}
	
