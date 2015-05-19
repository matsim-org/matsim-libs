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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;

/**
 * 
 * Computes the contribution of each transit line on the overall welfare changes.
 * 
 * First attempt: Allocate the changes in user benefits to the transit lines which are explicitly used by the user.
 * 
 * 
 * @author ikaddoura
 *
 */
public class WelfareAnalyzer {
	private static final Logger log = Logger.getLogger(WelfareAnalyzer.class);
	
	Map<Id<TransitLine>, Double> lineId2welfareCorreciton = new HashMap<>();
	Map<Id<Person>, Double> personId2benefitsBefore = new HashMap<>(); // previous iteration	
	
	public void computeWelfare(Scenario scenario) {
		
		Map<Id<Person>, Double> personId2benefits = new HashMap<>();
		
		for (Person person : scenario.getPopulation().getPersons().values()){
			
			// compute the user benefits and the difference to the previous iteration
			double benefits = person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
			personId2benefits.put(person.getId(), benefits);
			
			if (this.personId2benefitsBefore.containsKey(person.getId())){
				double benefitDifference = benefits - this.personId2benefitsBefore.get(person.getId());
				
				if (benefitDifference != 0.) {
					// allocate the difference in user benefits to the transit line
									
					List<Id<TransitLine>> usedTransitLines = new ArrayList<>();
					for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
						if (pE instanceof Leg) {
							Leg leg = (Leg) pE;
							if (leg.getMode().equals(TransportMode.pt)) {
								ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
								Id<TransitLine> transitLineId = route.getLineId();
								usedTransitLines.add(transitLineId);
							} 						
						}
					}
					
					// Problem: If a transit user uses several transit lines, all transit lines will be considered to have caused the improvement.
					// Solution: Find out due to which transit line each transit user is actually improved by (re-)scoring each part / leg.
					
					if (!usedTransitLines.isEmpty()) {
						double benefitDifferenceEachTransitLine = benefitDifference / usedTransitLines.size();
						
						for (Id<TransitLine> transitLineId : usedTransitLines) {
							if (!lineId2welfareCorreciton.containsKey(transitLineId)){
								lineId2welfareCorreciton.put(transitLineId, 0.);
							}
							double userBenefitContributionUpdatedValue = lineId2welfareCorreciton.get(transitLineId) + benefitDifferenceEachTransitLine;
							lineId2welfareCorreciton.put(transitLineId, userBenefitContributionUpdatedValue);
						}
						
					} else {
						
						log.warn("Changes in user benefits which are not allocated to transit lines.");
						log.warn("Problem: The increase in user benefits on other modes is not accounted for.");
						log.warn("Solution: Allocate the changes in user benefits to the transit lines which are 'responsible' for the improvement. Define responsibility by a relevant area around the transit line.");
					}
				}
				
			} else {
				log.warn("No score from previous (external) iteration. If this is the first (external) iteration everything is fine.");
			}
		}
		
		// save the scores for the next iteration
		personId2benefitsBefore = personId2benefits;
	}
	
	public double getLineId2welfareCorrection(Id<TransitLine> id) {
		if (this.lineId2welfareCorreciton.containsKey(id)){
			return this.lineId2welfareCorreciton.get(id);
		} else {
			return 0.;
		}
	}

}
