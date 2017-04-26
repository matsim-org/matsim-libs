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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.functions.ScoringParameters.Builder;
import org.matsim.utils.objectattributes.ObjectAttributes;

import floetteroed.utilities.Units;

/**
 * @author gunnar based on thibautd
 */
public class RandomizedScoringParameters implements
		ScoringParametersForPerson {
	private final PlanCalcScoreConfigGroup config;
	private final ScenarioConfigGroup scConfig;
	private final ObjectAttributes personAttributes;
	private final String subpopulationAttributeName;
	// private final Map<String, CharyparNagelScoringParameters> params = new
	// HashMap<>();
	private final Map<Person, ScoringParameters> params = new LinkedHashMap<>();

	private ScoringParameters defaultParameters = null;

	// @Inject
	RandomizedScoringParameters(PlansConfigGroup plansConfigGroup,
			PlanCalcScoreConfigGroup planCalcScoreConfigGroup,
			ScenarioConfigGroup scenarioConfigGroup, Population population) {
		this.config = planCalcScoreConfigGroup;
		this.scConfig = scenarioConfigGroup;
		this.personAttributes = population.getPersonAttributes();
		this.subpopulationAttributeName = plansConfigGroup
				.getSubpopulationAttributeName();
	}

	public RandomizedScoringParameters(Scenario scenario) {
		this(scenario.getConfig().plans(),
				scenario.getConfig().planCalcScore(), scenario.getConfig()
						.scenario(), scenario.getPopulation());
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {
		final String subpopulation = (String) personAttributes.getAttribute(
				person.getId().toString(), subpopulationAttributeName);

		if (!this.params.containsKey(person)) {
			// if (!this.params.containsKey(subpopulation)) {
			Builder builder = new Builder(this.config, this.config.getScoringParameters(subpopulation), scConfig);
			if (this.defaultParameters == null) {
				this.defaultParameters = builder.build();
			}
			builder = builder.setMarginalUtilityOfPerforming_s(this
					.marginalUtilityOfPerforming_s(person));
			builder = builder.setMarginalUtilityOfMoney(this
					.marginalUtilityOfMoney(person));
			builder = builder.setActivityParameters("home",
					this.actParams(person, "home"));
			builder = builder.setActivityParameters("work",
					this.actParams(person, "work"));
			this.params.put(person, builder.build());
		}

		// return this.params.get(subpopulation);
		return this.params.get(person);
	}

	// RANDOMIZATION

	private double getTypicalWorkDuration_s(final Person person) {
		final double duration_s = (Double) this.personAttributes.getAttribute(
				person.getId().toString(), "workduration_s");
		System.out.println("Person " + person.getId() + " wants to work for "
				+ (duration_s * Units.H_PER_S) + " hours.");
		return duration_s;
	}

	private double getTypicalHomeDuration_s(final Person person) {
		return Units.S_PER_D - this.getTypicalWorkDuration_s(person);
	}

	protected ActivityUtilityParameters.Builder actParams(final Person person,
			final String type) {
		final ActivityUtilityParameters defaultActParams = this.defaultParameters.utilParams
				.get(type);
		ActivityUtilityParameters.Builder builder = new ActivityUtilityParameters.Builder();
		builder.setType(defaultActParams.getType());
		builder.setPriority(1); // TODO no getter
		if ("work".equals(type)) {
			builder.setTypicalDuration_s(this.getTypicalWorkDuration_s(person));
		} else if ("home".equals(type)) {
			builder.setTypicalDuration_s(this.getTypicalHomeDuration_s(person));
		} else {
			throw new RuntimeException(type + " not known");
		}
		builder.setClosingTime(defaultActParams.getClosingTime());
		builder.setEarliestEndTime(defaultActParams.getEarliestEndTime());
		builder.setLatestStartTime(defaultActParams.getLatestStartTime());
		builder.setMinimalDuration(defaultActParams.getMinimalDuration());
		builder.setOpeningTime(defaultActParams.getOpeningTime());
		builder.setScoreAtAll(true); // TODO no getter
		builder.setZeroUtilityComputation(new ActivityUtilityParameters.SameAbsoluteScore());
		// TODO no getter
		return builder;
	}

	protected double marginalUtilityOfPerforming_s(final Person person) {
		return this.defaultParameters.marginalUtilityOfPerforming_s;
	}

	protected double marginalUtilityOfMoney(final Person person) {
		return this.defaultParameters.marginalUtilityOfMoney;
	}

}
