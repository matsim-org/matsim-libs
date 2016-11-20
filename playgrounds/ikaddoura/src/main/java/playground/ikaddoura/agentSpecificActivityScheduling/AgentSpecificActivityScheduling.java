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

package playground.ikaddoura.agentSpecificActivityScheduling;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.utils.prepare.PopulationTools;

/**
* @author ikaddoura
*/

public class AgentSpecificActivityScheduling {

	private static final Logger log = Logger.getLogger(AgentSpecificActivityScheduling.class);
	
	private double activityDurationBin = 3600.;
	private double tolerance = 900.;
	
	private boolean preparePopulation = true;
	private boolean removeNetworkSpecificInformation = false;
	private boolean keepOnlySelectedPlans = true;
	
	private String configFile = null;
	private Controler controler = null;
	
	public AgentSpecificActivityScheduling(String configFile) {
		this.configFile = configFile;
	}
	
	public AgentSpecificActivityScheduling(Controler controler) {
		this.controler = controler;
	}

	public Controler prepareControler() {
		
		Controler controler = null;
		
		if (configFile != null) {
			Config config = ConfigUtils.loadConfig(configFile);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			controler = new Controler(scenario);
		} else {
			if (this.controler == null) {
				throw new RuntimeException("Controler is null. Aborting...");
			}
			controler = this.controler;
		}
		
		// adjust population	
		if (preparePopulation) {
			
			log.info("Preparing the population...");

			if (keepOnlySelectedPlans) PopulationTools.getPopulationWithOnlySelectedPlans(controler.getScenario().getPopulation());
			
			PopulationTools.setActivityTypesAccordingToDurationAndMergeOvernightActivities(controler.getScenario().getPopulation(), activityDurationBin);			
			PopulationTools.addActivityTimesOfSelectedPlanToPersonAttributes(controler.getScenario().getPopulation());
			PopulationTools.analyze(controler.getScenario().getPopulation());
			
			if (removeNetworkSpecificInformation) PopulationTools.removeNetworkSpecificInformation(controler.getScenario().getPopulation());

			log.info("Preparing the population... Done.");					
		}
		
		// adjust config
		log.info("Adding duration-specific activity types to config...");
		
		List<ActivityParams> newActivityParams = new ArrayList<>();

		for (ActivityParams actParams : controler.getConfig().planCalcScore().getActivityParams()) {
			String activityType = actParams.getActivityType();
			
			if (activityType.contains("interaction")) {
				log.info("Skipping activity " + activityType + "...");
				
			} else {
				
				log.info("Splitting activity " + activityType + " in duration-specific activities.");

				double maximumDuration = (activityDurationBin * 24.) + activityDurationBin;
				
				for (double n = activityDurationBin; n <= maximumDuration ; n = n + activityDurationBin) {
					ActivityParams params = new ActivityParams(activityType + "_" + n);
					params.setTypicalDuration(n);
					params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
					newActivityParams.add(params);
				}
			}
		}
		
		for (ActivityParams actParams : newActivityParams) {
			controler.getConfig().planCalcScore().addActivityParams(actParams);
		}	
		
		log.info("Adding duration-specific activity types to config... Done.");
		
		// adjust scoring
		log.info("Replacing the default activity scoring by an agent-specific opening / closing time consideration...");
		
		final CountActEventHandler activityCounter = new CountActEventHandler();
		final AgentSpecificScoringFunctionFactory scoringFunctionFactory = new AgentSpecificScoringFunctionFactory(controler.getScenario(), activityCounter, tolerance);
		
		controler.addOverridingModule( new AbstractModule() {
			
			@Override
			public void install() {
						
				this.addEventHandlerBinding().toInstance(activityCounter);
				this.bindScoringFunctionFactory().toInstance(scoringFunctionFactory);				
			}
		});
		
		log.info("Replacing the default activity scoring by an agent-specific opening / closing time consideration... Done.");
		
		return controler;
	}
	
	public void setPreparePopulation(boolean preparePopulation) {
		this.preparePopulation = preparePopulation;
	}

	public void setActivityDurationBin(double activityDurationBin) {
		this.activityDurationBin = activityDurationBin;
	}

	public void setRemoveNetworkSpecificInformation(boolean removeNetworkSpecificInformation) {
		this.removeNetworkSpecificInformation = removeNetworkSpecificInformation;
	}

	public void setKeepOnlySelectedPlans(boolean keepOnlySelectedPlans) {
		this.keepOnlySelectedPlans = keepOnlySelectedPlans;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

}

