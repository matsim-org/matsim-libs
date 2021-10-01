/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class SubpopulationScoringParameters implements ScoringParametersForPerson {
	private final PlanCalcScoreConfigGroup config;
	private final ScenarioConfigGroup scConfig;
	private final TransitConfigGroup transitConfigGroup;
	private final Map<String, ScoringParameters> params = new HashMap<>();
	private final Population population;

	@Inject
	SubpopulationScoringParameters(PlansConfigGroup plansConfigGroup, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ScenarioConfigGroup scenarioConfigGroup, Population population, TransitConfigGroup transitConfigGroup) {
		this.config = planCalcScoreConfigGroup;
		this.scConfig = scenarioConfigGroup;
		this.transitConfigGroup = transitConfigGroup;
		this.population = population ;
	}

	public SubpopulationScoringParameters(Scenario scenario) {
		this(scenario.getConfig().plans(), scenario.getConfig().planCalcScore(), scenario.getConfig().scenario(), scenario.getPopulation(), scenario.getConfig().transit());
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {
//		final String subpopulation = (String) PopulationUtils.getPersonAttribute( person, subpopulationAttributeName) ;
		final String subpopulation = PopulationUtils.getSubpopulation( person );

		if (!this.params.containsKey(subpopulation)) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
			 * end up with different params-object, although all objects will have the same
			 * values in them due to using the same config. Still much better from a memory performance
			 * point of view than giving each ScoringFunction its own copy of the params.
			 */
			ScoringParameters.Builder builder = new ScoringParameters.Builder(this.config, this.config.getScoringParameters(subpopulation), scConfig);
			if (transitConfigGroup.isUseTransit()) {
				// yyyy this should go away somehow. :-)



				PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
				transitActivityParams.setTypicalDuration(120.0);
				transitActivityParams.setOpeningTime(0.) ;
				transitActivityParams.setClosingTime(0.) ;
				ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
				modeParamsBuilder.setScoreAtAll(false);
				builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder.build());
			}

			this.params.put(
					subpopulation,
					builder.build());
		}

		return this.params.get(subpopulation);
	}
}
