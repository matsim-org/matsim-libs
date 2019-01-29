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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.pt.routes.ExperimentalTransitRoute;

import com.google.inject.Inject;

/**
 * To have an example/prototype and for testing purposes.
 * This subsidy approach simply adds a certain amount for each passenger to the operator's score.
 * 
 * TODO: Compute the contribution of each transit line on the overall welfare changes.
 * Allocates the changes in user benefits to the transit lines which are used by the user.
 * 
 * 
 * @author ikaddoura
 *
 */
public class PerPassengerSubsidy implements SubsidyI {

//	private Map<Id<Person>, Double> personId2initialBenefits;
//	private Map<Id<Person>, Double> personId2benefits;
//	private Map<Id<Person>, Set<Id<PPlan>>> personId2usedPPlanIds;
//	private Set<Id<PPlan>> currentPPlanTransitRouteIds;
	
	private Map<Id<PPlan>, Double> welfareCorrection;
	private Map<Id<PPlan>, Set<Id<Person>>> pId2persons;

	private final double subsidyPerPassenger = 100000.;
	
	@Inject
	Scenario scenario;
	
	public PerPassengerSubsidy(){
		// yyyy I am not happy with passing a string here and having the reader inside this method.  kai, mar'17
		
		//get initial scores from the given file and store the values
//		this.personId2initialBenefits = new HashMap<>();
//		
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		
//		new PopulationReader(scenario).readFile(initialScoresFile);
//		
//		for(Person person : scenario.getPopulation().getPersons().values()){
//			
//			this.personId2initialBenefits.put(person.getId(), person.getSelectedPlan().getScore());
//			
//		}
//		
//		log.info("Initial scores for " + scenario.getPopulation().getPersons().size() + " persons have been successfully stored.");
		
	}
	
	@Override
	public void computeSubsidy() {
		// yyyy welfare should be computed based on executed plans, not planned plans.  --> use ExecutedPlansService
		// When this is injected, we do not need scenario as an argument here so every implementor can do what she wants.
		
		// Initialize all maps.
//		this.personId2usedPPlanIds = new HashMap<>();
//		this.personId2benefits = new HashMap<>();

//		this.currentPPlanTransitRouteIds = new HashSet<Id<PPlan>>();
		this.welfareCorrection = new HashMap<>();
		this.pId2persons = new HashMap<>();

//		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()){
//			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
//				String planIdString = transitRoute.getId().toString();
//				Id<PPlan> planId = Id.create(planIdString, PPlan.class);
//				this.currentPPlanTransitRouteIds.add(planId);
//			}	
//		}	
		
		// Go through the entire population.
		for (Person person : scenario.getPopulation().getPersons().values()){
			
			// Get the PPlan which is used by this person.
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				
				if (pE instanceof Leg) {
					
					Leg leg = (Leg) pE;
					
					if (leg.getMode().equals(TransportMode.pt)) {

						ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
						String planIdString = route.getRouteId().toString();
						Id<PPlan> pId = Id.create(planIdString, PPlan.class);
						
						if (this.pId2persons.get(pId) != null) {
							this.pId2persons.get(pId).add(person.getId());
						} else {
							Set<Id<Person>> persons = new HashSet<>();
							persons.add(person.getId());
							this.pId2persons.put(pId, persons);
						}

//						if(!this.personId2usedPPlanIds.containsKey(person.getId())){
//							this.personId2usedPPlanIds.put(person.getId(), new HashSet<Id<PPlan>>());
//						}
//						
//						this.personId2usedPPlanIds.get(person.getId()).add(planId);
						// yyyy this will in the end give the welfare benefit to the pplan that was _planned_ to be used. 
						// Not necessarily to the one that _was_ actually used. kai, jan'17

					}
				}
			}
						
			// Compute the user benefits and the difference to the initial iteration.
//			double benefits = person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
//			personId2benefits.put(person.getId(), benefits);
//			
//			double benefitsInitialIteration = this.personId2initialBenefits.get(person.getId());
//			
//			if(benefitsInitialIteration == 0){
//				
//				log.warn("There is no information about the user benefits of person " + person.getId() + " in the initial iteration. Setting the benefits in the previous iteration to " + benefitsInitialIteration + ".");
//				benefitsInitialIteration = -120.;
//				
//			}
//				
//			double benefitDifference = benefits - benefitsInitialIteration;
//			
//			if (benefitDifference > 0.) {
//				// allocate the increase in user benefits to the transit routes (PPlanIDs)
//				
//				// Problem: If a transit user uses several transit lines, all transit lines will be considered to have caused the improvement.
//				// TODO: Solution: Find out due to which transit line each transit user is actually improved by (re-)scoring each part / leg.
//						
//				if (personId2usedPPlanIds.containsKey(person.getId())) {
//					double benefitDifferenceEachTransitLine = benefitDifference / personId2usedPPlanIds.get(person.getId()).size();
//					
//					for (Id<PPlan> planId : personId2usedPPlanIds.get(person.getId())) {
//						if (!planId2welfareCorrection.containsKey(planId)){
//							planId2welfareCorrection.put(planId, 0.);
//						}
//						double userBenefitContributionUpdatedValue = planId2welfareCorrection.get(planId) + benefitDifferenceEachTransitLine;
//						planId2welfareCorrection.put(planId, userBenefitContributionUpdatedValue);
//					}
//				} else {
//					
//					log.warn("Changes in user benefits which are not allocated to transit lines.");
//					log.warn("Problem: The increase in user benefits on other modes is not accounted for.");
//					log.warn("Solution: Allocate the changes in user benefits to the transit lines which are 'responsible' for the improvement. Define responsibility by a relevant area around the transit line.");
//					
//				}
//				
//			} else {
//				// the change in user benefits is zero or negative
//			}
		}
		
		for (Id<PPlan> id : this.pId2persons.keySet()) {
			this.welfareCorrection.put(id, pId2persons.get(id).size() * subsidyPerPassenger);
		}
	}
	
	public void writeToFile(ScoringEvent event) {
		// yyyy find other solution.  This would force every implementer to implement something here. 
		
//		String delimiter = ";";
//		
//		// agent stats (agent id, benefits of this iteration, pplan ids the agent used in this iteration)
//		BufferedWriter personStatsWriter = IOUtils.getBufferedWriter(event.getServices().getControlerIO().getIterationPath(event.getIteration()) + "/welfarePersonStats." + Integer.toString(event.getIteration()) + ".txt");
//		
//		Map<Id<PPlan>,Set<Id<Person>>> usedPlans = new HashMap<>();
//		
//		try {
//			
//			personStatsWriter.write("personId" + delimiter + "benefits" + delimiter + "used_pplans");
//			
//			for(Entry<Id<Person>, Double> benefitEntry : this.personId2benefits.entrySet()){
//				
//				personStatsWriter.newLine();
//				
//				String id = benefitEntry.getKey().toString();
//				String score = Double.toString(benefitEntry.getValue());
//				
//				StringBuffer stB = new StringBuffer();
//				Set<Id<PPlan>> usedPPlanIds = this.personId2usedPPlanIds.get(benefitEntry.getKey());
//				if(usedPPlanIds != null){
//					for(Id<PPlan> planId : usedPPlanIds){
//						
//						if(!usedPlans.containsKey(planId)){
//							
//							usedPlans.put(planId, new HashSet<Id<Person>>());
//							
//						}
//						
//						usedPlans.get(planId).add(benefitEntry.getKey());
//						
//						stB.append(planId + ",");
//					}
//				}
//				
//				personStatsWriter.write(id + delimiter + score + delimiter + stB.toString());
//				
//			}
//			
//			personStatsWriter.flush();
//			personStatsWriter.close();
//			
//		} catch (IOException e) {
//			
//			e.printStackTrace();
//			
//		}
//		
//		//pplan stats (id, revenues from subsidies, agent ids that used the pplan in this iteration)
//		BufferedWriter planStatsWriter = IOUtils.getBufferedWriter(event.getServices().getControlerIO().getIterationPath(event.getIteration()) + "/welfarePPlanStats." + Integer.toString(event.getIteration()) + ".txt");
//		
//		try {
//			
//			planStatsWriter.write("pplan_id" + delimiter + "revenues" + delimiter + "user_ids");
//			
//			for(Id<PPlan> pplanId : this.currentPPlanTransitRouteIds){
//				
//				planStatsWriter.newLine();
//				
//				String id = pplanId.toString();
//				String value = Double.toString(this.getWelfareCorrection(pplanId));
//				
//				StringBuffer stB = new StringBuffer();
//				
//				for(Entry<Id<Person>,Set<Id<PPlan>>> usedPPlanEntry : this.personId2usedPPlanIds.entrySet()){
//					
//					for(Id<PPlan> planId : usedPPlanEntry.getValue()){
//						
//						if(planId.toString().equals(id)){
//							
//							stB.append(usedPPlanEntry.getKey().toString() + ",");
//							break;
//							
//						}
//						
//					}
//					
//				}
//				
//				planStatsWriter.write(id + delimiter + value + delimiter + stB.toString());
//				
//			}
//			
//			planStatsWriter.flush();
//			planStatsWriter.close();
//		
//		} catch (IOException e) {
//			
//			e.printStackTrace();
//			
//		}
			
	}

	@Override
	public double getSubsidy(Id<PPlan> pPlanId) {
		if (this.welfareCorrection.get(pPlanId) != null){
			return this.welfareCorrection.get(pPlanId);
			
		} else {
			return 0.;
		}
	}

}
