/* *********************************************************************** *
 * project: org.matsim.*
 * CountEqualPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author benjamin
 *
 */
public class CountEqualPlans {
	private static final Logger logger = Logger.getLogger(CountEqualPlans.class);
	
	String plansFile = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/output_plans.xml.gz";
	String netFile = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/output_network.xml.gz";
	
	int totalNumberOfPersons = 0;
	int totalNumberOfNonSelectedPlans = 0;
	int legModeCombinationCounter = 0;
	int equalCounter = 0;
	int planContainsCarLegCounter = 0;

	private void run() {
		Scenario sc = loadScenario();
		Population pop = sc.getPopulation();
		for(Person person : pop.getPersons().values()){
			this.totalNumberOfPersons ++;
			Plan selectedPlan = person.getSelectedPlan();
			planContainsCarLeg(selectedPlan);
			List<? extends Plan> planList = person.getPlans();
			for(Plan plan : planList){
				if(plan.equals(selectedPlan)){
					// do nothing
				} else {
					this.totalNumberOfNonSelectedPlans ++;
					if(isLegModeCombinationEqual(selectedPlan, plan)){
						this.legModeCombinationCounter ++;
						if(planContainsCarLeg(plan)){
							if(pathIsEqual(selectedPlan, plan)){
								this.equalCounter ++;
							}
						}
					}
				}
			}
		}
		logger.info("The population consists of " + this.totalNumberOfPersons + " agents which have " + this.totalNumberOfNonSelectedPlans + " non-selected plans.");
		logger.info("Out of these plans, " + this.legModeCombinationCounter + " have the same legmode combination as the selected plan.");
		logger.info("Out of these plans, " + this.planContainsCarLegCounter + " have at least one car leg.");
		logger.info("Out of these plans, " + this.equalCounter + " are equal to the selected plan.");
	}
	
	private boolean pathIsEqual(Plan selectedPlan, Plan plan) {
		boolean pathIsEqual = false;
		List<Id> selectedLinks = new ArrayList<Id>();
		List<Id> planLinks = new ArrayList<Id>();
		for(PlanElement pe : selectedPlan.getPlanElements()){
			if(pe instanceof Leg){
				Leg leg = (Leg) pe;
				if(leg.getRoute() instanceof NetworkRoute){
					NetworkRoute selectedRoute = (NetworkRoute) leg.getRoute();
					for (Id linkId : selectedRoute.getLinkIds()){
						selectedLinks.add(linkId);
					}
				} else {
					// do nothing
				}
			}
		}
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof Leg){
				Leg leg = (Leg) pe;
				if(leg.getRoute() instanceof NetworkRoute){
					NetworkRoute planRoute = (NetworkRoute) leg.getRoute();
					for (Id linkId : planRoute.getLinkIds()){
						planLinks.add(linkId);
					}
				} else {
					// do nothing
				}
			}
		}
		if(selectedLinks.size() != 0 && planLinks.size() != 0 ){
			if(selectedLinks.size() == planLinks.size()){
				for(int i=0; i<selectedLinks.size(); i++){
					if(selectedLinks.get(i).equals(planLinks.get(i))){
						pathIsEqual = true;
					} else {
						pathIsEqual = false;
						break;
					}
				}
			}
		}
		return pathIsEqual;
	}

	private boolean planContainsCarLeg(Plan plan) {
		boolean planContainsCarLeg = false;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof Leg){
				Leg leg = (Leg) pe;
				String legMode = leg.getMode();
				if(legMode.equals("car")){
					this.planContainsCarLegCounter ++;
					planContainsCarLeg = true;
					break;
				}
			}
		}
		return planContainsCarLeg;
	}

	private boolean isLegModeCombinationEqual(Plan selectedPlan, Plan plan) {
		boolean isLegModeCombinationEqual = false;

//		double selectedScore = selectedPlan.getScore();
//		double planScore = plan.getScore();
//		if(selectedScore == planScore){
//			isPlanEqual = true;
//		}
		
		List<String> selectedModes = new ArrayList<String>();
		List<String> planModes = new ArrayList<String>();
		for(PlanElement pe : selectedPlan.getPlanElements()){
			if(pe instanceof Leg){
				String modeSelected = ((Leg) pe).getMode();
				selectedModes.add(modeSelected);
			}
		}
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof Leg){
				String modePlan = ((Leg) pe).getMode();
				planModes.add(modePlan);
			}
		}
		for(int i=0; i<selectedModes.size(); i++){
			if(selectedModes.get(i).equals(planModes.get(i))){
				isLegModeCombinationEqual = true;
			} else {
				isLegModeCombinationEqual = false;
				break;
			}
		}
		return isLegModeCombinationEqual;
	}

	private Scenario loadScenario() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public static void main(String[] args) {
		new CountEqualPlans().run();
	}
}
