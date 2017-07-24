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
package org.matsim.contrib.opdyts.useCases.modeChoice;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * This is based on {@link org.matsim.core.scoring.functions.SubpopulationScoringParameters} . The difference is that the "params"
 * field is recreated in each iteration, thus being able to pick up changes inserted into the config scoring parameters by Opdyts.
 * 
 * @author Kai Nagel based on thibautd
 */
public class EveryIterationScoringParameters implements ScoringParametersForPerson {
	private static final Logger log = Logger.getLogger(EveryIterationScoringParameters.class);

	private final PlanCalcScoreConfigGroup config;
	private final ScenarioConfigGroup scConfig;
	private final TransitConfigGroup transitConfigGroup;
	private final ObjectAttributes personAttributes;
	private final String subpopulationAttributeName;
	private final Map<String, ScoringParameters> params = new HashMap<>();
	private ReplanningContext context;
	private int previousIteration = -1;

	@Inject
    EveryIterationScoringParameters(PlansConfigGroup plansConfigGroup,
                                    PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ScenarioConfigGroup scenarioConfigGroup,
                                    Population population, TransitConfigGroup transitConfigGroup, ReplanningContext context) {
		this.config = planCalcScoreConfigGroup;
		this.scConfig = scenarioConfigGroup;
		this.transitConfigGroup = transitConfigGroup;
		this.context = context;
		this.personAttributes = population.getPersonAttributes();
		this.subpopulationAttributeName = plansConfigGroup.getSubpopulationAttributeName();
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {
		boolean flag = false;
		if (context.getIteration() > previousIteration) {
			previousIteration = context.getIteration();
			params.clear();
			flag = true;
		}

		final String subpopAttribName = (String) personAttributes.getAttribute(person.getId().toString(),
				subpopulationAttributeName);

		if (!this.params.containsKey(subpopAttribName)) {
			/*
			 * lazy initialization of params. not strictly thread safe, as different threads could end up with different
			 * params-object, although all objects will have the same values in them due to using the same config. Still much
			 * better from a memory performance point of view than giving each ScoringFunction its own copy of the params.
			 */
			ScoringParameters.Builder builder = new ScoringParameters.Builder(this.config,
					this.config.getScoringParameters(subpopAttribName), scConfig);
			if (transitConfigGroup.isUseTransit()) {
				// yyyy this should go away somehow. :-)

				PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(
						PtConstants.TRANSIT_ACTIVITY_TYPE);
				transitActivityParams.setTypicalDuration(120.0);
				transitActivityParams.setOpeningTime(0.);
				transitActivityParams.setClosingTime(0.);
				ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(
						transitActivityParams);
				modeParamsBuilder.setScoreAtAll(false);
				builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder);
			}

			this.params.put(subpopAttribName, builder.build());
		}

		if (flag) {
			flag = false;
			for (Entry<String, ModeUtilityParameters> entry : this.params.get(subpopAttribName).modeParams
					.entrySet()) {
				final String mode = entry.getKey();
				if (TransportMode.car.equals(mode) || TransportMode.pt.equals(mode)) {
					log.warn(mode + ": " + entry.getValue().constant + " + "
							+ (entry.getValue().marginalUtilityOfTraveling_s * 3600.) + " * ttime ; ");
				}
			}
		}

		return this.params.get(subpopAttribName);
	}
}
