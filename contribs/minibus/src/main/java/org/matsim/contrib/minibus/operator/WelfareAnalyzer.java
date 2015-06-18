/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.minibus.operator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * 
 * Computes the contribution of each transit line on the overall welfare changes.
 * Allocates the changes in user benefits to the transit lines which are used by the user.
 * 
 * 
 * @author ikaddoura
 *
 */
public class WelfareAnalyzer {
	private static final Logger log = Logger.getLogger(WelfareAnalyzer.class);

	private Map<Id<Person>, Double> personId2initialBenefits;	
	private Map<Id<PPlan>, Double> planId2welfareCorrection;
	private Map<Id<Person>, Double> personId2benefits;
	private Map<Id<Person>, Set<Id<PPlan>>> personId2usedPPlanIds;
	private Set<Id<PPlan>> currentPPlanIds;
	
	public WelfareAnalyzer(String initialScoresFile){
		
		//get initial scores from the given file and store the values
		this.personId2initialBenefits = new HashMap<>();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimPopulationReader(scenario).readFile(initialScoresFile);
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			this.personId2initialBenefits.put(person.getId(), person.getSelectedPlan().getScore());
			
		}
		
		log.info("Initial scores for " + scenario.getPopulation().getPersons().size() + " persons have been successfully stored.");
		
	}
	
	public void computeWelfare(Scenario scenario) {

		// Initialize all maps.
		this.personId2usedPPlanIds = new HashMap<>();
		this.personId2benefits = new HashMap<>();
		this.planId2welfareCorrection = new HashMap<>();
		this.currentPPlanIds = new HashSet<Id<PPlan>>();
		
		setCurrentPPlanIds(scenario);
		
		// Go through the entire population.
		for (Person person : scenario.getPopulation().getPersons().values()){
			
			// Get the PPlan which is used by this person.
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				
				if (pE instanceof Leg) {
					
					Leg leg = (Leg) pE;
					
					if (leg.getMode().equals(TransportMode.pt)) {

						ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
						String planIdString = route.getRouteId().toString();//.replace(route.getLineId().toString() + "-", "");
						Id<PPlan> planId = Id.create(planIdString, PPlan.class);

						if(!this.personId2usedPPlanIds.containsKey(person.getId())){
							this.personId2usedPPlanIds.put(person.getId(), new HashSet<Id<PPlan>>());
						}
						
						this.personId2usedPPlanIds.get(person.getId()).add(planId);

					}
				}
			}
						
			// Compute the user benefits and the difference to the initial iteration.
			double benefits = person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
			personId2benefits.put(person.getId(), benefits);
			
			double benefitsInitialIteration = this.personId2initialBenefits.get(person.getId());
			
			if(benefitsInitialIteration == 0){
				
				log.warn("There is no information about the user benefits of person " + person.getId() + " in the initial iteration. Setting the benefits in the previous iteration to " + benefitsInitialIteration + ".");
				benefitsInitialIteration = -120;
				
			}
				
			double benefitDifference = benefits - benefitsInitialIteration;
			
			if (benefitDifference != 0.) {
				// allocate the difference in user benefits to the transit routes (PPlanIDs)
				
				// Problem: If a transit user uses several transit lines, all transit lines will be considered to have caused the improvement.
				// TODO: Solution: Find out due to which transit line each transit user is actually improved by (re-)scoring each part / leg.
						
				if (personId2usedPPlanIds.containsKey(person.getId())) {
					double benefitDifferenceEachTransitLine = benefitDifference / personId2usedPPlanIds.get(person.getId()).size();
					
					for (Id<PPlan> planId : personId2usedPPlanIds.get(person.getId())) {
						if (!planId2welfareCorrection.containsKey(planId)){
							planId2welfareCorrection.put(planId, 0.);
						}
						double userBenefitContributionUpdatedValue = planId2welfareCorrection.get(planId) + benefitDifferenceEachTransitLine;
						planId2welfareCorrection.put(planId, userBenefitContributionUpdatedValue);
					}
				} else {
					
					log.warn("Changes in user benefits which are not allocated to transit lines.");
					log.warn("Problem: The increase in user benefits on other modes is not accounted for.");
					log.warn("Solution: Allocate the changes in user benefits to the transit lines which are 'responsible' for the improvement. Define responsibility by a relevant area around the transit line.");
					
				}
				
			} else {
				// the change in user benefits is zero
			}
		}		
	}

	private void setCurrentPPlanIds(Scenario scenario) {
				
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()){
			
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				
				String planIdString = transitRoute.getId().toString();
				Id<PPlan> planId = Id.create(planIdString, PPlan.class);
				this.currentPPlanIds.add(planId);
			}	
		}	
	}
	
	public double getLineId2welfareCorrection(Id<PPlan> id) {
		
		if (this.planId2welfareCorrection.containsKey(id)){
			
			return this.planId2welfareCorrection.get(id);
			
		} else {
			
			return 0.;
			
		}
		
	}

	public void writeToFile(ScoringEvent event) {
		
		String delimiter = ";";
		
		// agent stats (agent id, benefits of this iteration, pplan ids the agent used in this iteration)
		BufferedWriter personStatsWriter = IOUtils.getBufferedWriter(event.getControler().getControlerIO().getIterationPath(event.getIteration()) + "/welfarePersonStats." + Integer.toString(event.getIteration()) + ".txt");
		
		Map<Id<PPlan>,Set<Id<Person>>> usedPlans = new HashMap<>();
		
		try {
			
			personStatsWriter.write("personId" + delimiter + "benefits" + delimiter + "used_pplans");
			
			for(Entry<Id<Person>, Double> benefitEntry : this.personId2benefits.entrySet()){
				
				personStatsWriter.newLine();
				
				String id = benefitEntry.getKey().toString();
				String score = Double.toString(benefitEntry.getValue());
				
				StringBuffer stB = new StringBuffer();
				Set<Id<PPlan>> usedPPlanIds = this.personId2usedPPlanIds.get(benefitEntry.getKey());
				if(usedPPlanIds != null){
					for(Id<PPlan> planId : usedPPlanIds){
						
						if(!usedPlans.containsKey(planId)){
							
							usedPlans.put(planId, new HashSet<Id<Person>>());
							
						}
						
						usedPlans.get(planId).add(benefitEntry.getKey());
						
						stB.append(planId + ",");
					}
				}
				
				personStatsWriter.write(id + delimiter + score + delimiter + stB.toString());
				
			}
			
			personStatsWriter.flush();
			personStatsWriter.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		//pplan stats (id, revenues from subsidies, agent ids that used the pplan in this iteration)
		BufferedWriter planStatsWriter = IOUtils.getBufferedWriter(event.getControler().getControlerIO().getIterationPath(event.getIteration()) + "/welfarePPlanStats." + Integer.toString(event.getIteration()) + ".txt");
		
		try {
			
			planStatsWriter.write("pplan_id" + delimiter + "revenues" + delimiter + "user_ids");
			
			for(Id<PPlan> pplanId : this.currentPPlanIds){
				
				planStatsWriter.newLine();
				
				String id = pplanId.toString();
				String value = Double.toString(this.getLineId2welfareCorrection(pplanId));
				
				StringBuffer stB = new StringBuffer();
				
				for(Entry<Id<Person>,Set<Id<PPlan>>> usedPPlanEntry : this.personId2usedPPlanIds.entrySet()){
					
					for(Id<PPlan> planId : usedPPlanEntry.getValue()){
						
						if(planId.toString().equals(id)){
							
							stB.append(usedPPlanEntry.getKey().toString() + ",");
							break;
							
						}
						
					}
					
				}
				
				planStatsWriter.write(id + delimiter + value + delimiter + stB.toString());
				
			}
			
			planStatsWriter.flush();
			planStatsWriter.close();
		
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
				
	}

}
