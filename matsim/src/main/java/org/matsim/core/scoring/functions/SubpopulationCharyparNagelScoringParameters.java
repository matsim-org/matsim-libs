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
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributes;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class SubpopulationCharyparNagelScoringParameters implements CharyparNagelScoringParametersForPerson {
	private final PlanCalcScoreConfigGroup config;
	private final ScenarioConfigGroup scConfig;
	private final ObjectAttributes personAttributes;
	private final String subpopulationAttributeName;
	private final Map<String, CharyparNagelScoringParameters> params = new HashMap<>();

	@Inject
	SubpopulationCharyparNagelScoringParameters(Config config, Population population) {
		this.config = config.planCalcScore();
		this.scConfig = config.scenario();
		this.personAttributes = population.getPersonAttributes();
		this.subpopulationAttributeName = config.plans().getSubpopulationAttributeName();
	}

	public SubpopulationCharyparNagelScoringParameters(Scenario scenario) {
		this(scenario.getConfig(), scenario.getPopulation());
	}

	@Override
	public CharyparNagelScoringParameters getScoringParameters(Person person) {
		final String subpopulation = (String) personAttributes.getAttribute(
				person.getId().toString(),
				subpopulationAttributeName);

		if (!this.params.containsKey(subpopulation)) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
* end up with different params-object, although all objects will have the same
* values in them due to using the same config. Still much better from a memory performance
* point of view than giving each ScoringFunction its own copy of the params.
*/
			this.params.put(
					subpopulation,
					CharyparNagelScoringParameters.getBuilder(
							this.config,
							this.config.getScoringParameters(subpopulation),
							scConfig).create());
		}

		return this.params.get(subpopulation);
	}
}
