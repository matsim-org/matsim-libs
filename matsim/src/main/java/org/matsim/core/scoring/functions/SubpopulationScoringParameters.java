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
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author thibautd
 */
public class SubpopulationScoringParameters implements ScoringParametersForPerson {

	private static final String DEFAULT_SUBPOPULATION_KEY = "placeholder-for-null-subpopulation";

	private final ScoringConfigGroup config;
	private final ScenarioConfigGroup scConfig;
	private final TransitConfigGroup transitConfigGroup;
	// Use concurrent hash map, as getScoringParameters might be called from multiple threads.
	private final Map<String, ScoringParameters> params = new ConcurrentHashMap<>() {
	};

	@Inject
	SubpopulationScoringParameters(ScoringConfigGroup scoringConfigGroup, ScenarioConfigGroup scenarioConfigGroup, TransitConfigGroup transitConfigGroup) {
		this.config = scoringConfigGroup;
		this.scConfig = scenarioConfigGroup;
		this.transitConfigGroup = transitConfigGroup;
	}

	public SubpopulationScoringParameters(Scenario scenario) {
		this(scenario.getConfig().scoring(), scenario.getConfig().scenario(), scenario.getConfig().transit());
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {

		String subpopulation = PopulationUtils.getSubpopulation( person );
		// concurrent hash map does not allow for null keys, so use a placeholder.
		String key = subpopulation == null ? DEFAULT_SUBPOPULATION_KEY : subpopulation;
		// Use atomic computeIfAbsent, to ensure consistent results when being called from multiple threads.
		return this.params.computeIfAbsent(key, this::newScoringParameters);
	}

	private ScoringParameters newScoringParameters(String subpopulation) {
		ScoringParameters.Builder builder = new ScoringParameters.Builder(this.config, this.config.getScoringParameters(subpopulation), scConfig);
		if (transitConfigGroup.isUseTransit()) {
			// yyyy this should go away somehow. :-)
			ScoringConfigGroup.ActivityParams transitActivityParams = new ScoringConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
			transitActivityParams.setTypicalDuration(120.0);
			transitActivityParams.setOpeningTime(0.) ;
			transitActivityParams.setClosingTime(0.) ;
			ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
			modeParamsBuilder.setScoreAtAll(false);
			builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder.build());
		}
		return builder.build();
	}
}
