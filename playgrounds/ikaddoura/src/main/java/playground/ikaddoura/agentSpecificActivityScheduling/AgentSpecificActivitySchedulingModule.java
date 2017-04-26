/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.controler.AbstractModule;

import playground.ikaddoura.utils.prepare.PopulationTools;

/**
 * 
 * Idea: Interpret the activity times in the initial population as agent-specific activity opening, closing and latest start times.
 * This allows to switch on time allocation and to adjust the elasticity via beta_performing, beta_late and the tolerance value.
 * 
 * (1) Optional: Adjusts the population in the scenario:
 * 		(a) converts normal activities (e.g. home, work) to duration-specific activities (e.g. home_7200, work_3600, ...)
 * 		(b) merges overnight activities in case they have the same base type (e.g. home_7200 and home_3600 --> home_10800) 
 * 		(c) writes initial activity times to person attributes. The initial activity schedule will be used to compute agent-specific opening, closing and latest arrival times.
 * (2) Adjusts the config to account for the new activity types (the typical duration is set according to the duration in the initial situation).
 * (3) Replaces the default activity scoring by an agent-specific activity scoring function which accounts for the opening/closing/latest start times in the person attributes.
 * 
 * (Other activity parameters will be ignored. The activity-related score only results from performing an activity plus an additional late arrival penalty which comes on top of the opportunity cost of time.)
 * 
 * 
* @author ikaddoura
*/

public class AgentSpecificActivitySchedulingModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(AgentSpecificActivitySchedulingModule.class);	
	private final List<ActivityParams> initialActivityParams = new ArrayList<>();
	private final AgentSpecificActivitySchedulingConfigGroup asasConfigGroup;
	
	public AgentSpecificActivitySchedulingModule(Scenario scenario) {

		asasConfigGroup = (AgentSpecificActivitySchedulingConfigGroup) scenario.getConfig().getModules().get(AgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);
		
		if (asasConfigGroup.isUseAgentSpecificActivityScheduling()) {
			adjustConfig(scenario.getConfig());
			
			if (asasConfigGroup.isAdjustPopulation()) {
				log.info("Adjusting the population...");
				
				Population population = scenario.getPopulation();
				
				if (population != null) {
					
					PopulationTools.setScoresToZero(population);
					PopulationTools.setActivityTypesAccordingToDurationAndMergeOvernightActivities(population, asasConfigGroup.getActivityDurationBin());			
					PopulationTools.addActivityTimesOfSelectedPlanToPersonAttributes(population);
					PopulationTools.analyze(population);
						
					if (asasConfigGroup.isRemoveNetworkSpecificInformation()) PopulationTools.removeNetworkSpecificInformation(population);

				} else {
					throw new RuntimeException("Cannot adjust the population if the population is null. Aborting...");
				}
				
				log.info("Adjusting the population... Done.");
			
			} else {
				log.info("Not adjusting the population."
						+ " Opening and closing times are expected to be provided as person attributes in the plans file.");
			}
			
		} else {
			log.info("Agent-specific activity scheduling disabled. Config and population are not adjusted.");
		}
	}

	private void adjustConfig(Config config) {
		
		AgentSpecificActivitySchedulingConfigGroup asasConfigGroup = (AgentSpecificActivitySchedulingConfigGroup) config.getModules().get(AgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);
		
		log.info("Initial activities: ");
		for (ActivityParams actParams : config.planCalcScore().getActivityParams()) {
			initialActivityParams.add(actParams);
			log.info(" -> " + actParams.getActivityType());
		}
		
		log.info("Adding duration-specific activity types to config...");
		
		List<ActivityParams> newActivityParams = new ArrayList<>();
		
		for (ActivityParams actParams : initialActivityParams) {
			String activityType = actParams.getActivityType();
			
			if (activityType.contains("interaction")) {
				log.info("Skipping activity " + activityType + "...");
				
			} else {
				
				log.info("Splitting activity " + activityType + " in duration-specific activities.");

				double maximumDuration = (asasConfigGroup.getActivityDurationBin() * 24.) + asasConfigGroup.getActivityDurationBin();
				
				for (double n = asasConfigGroup.getActivityDurationBin(); n <= maximumDuration ; n = n + asasConfigGroup.getActivityDurationBin()) {
					ActivityParams params = new ActivityParams(activityType + "_" + n);
					params.setTypicalDuration(n);
					params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
					newActivityParams.add(params);
				}
			}
		}
		
		for (ActivityParams actParams : newActivityParams) {
			config.planCalcScore().addActivityParams(actParams);
		}	
		
		log.info("Adding duration-specific activity types to config... Done.");
	}

	@Override
	public void install() {
		
		if (asasConfigGroup.isUseAgentSpecificActivityScheduling()) {
			// adjust scoring
			log.info("Replacing the default activity scoring by an agent-specific opening / closing time consideration...");
					
			this.bind(CountActEventHandler.class).asEagerSingleton();
			this.addEventHandlerBinding().to(CountActEventHandler.class);
			
			this.bindScoringFunctionFactory().to(AgentSpecificScoringFunctionFactory.class);	
			
			log.info("Replacing the default activity scoring by an agent-specific opening / closing time consideration... Done.");
		
		} else {
			log.info("Agent-specific activity scheduling disabled. Using the default scoring.");
		}
	}

}

