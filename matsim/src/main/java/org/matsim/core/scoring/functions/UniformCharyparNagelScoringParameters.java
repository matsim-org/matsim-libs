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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;

/**
 * @author thibautd
 */
public class UniformCharyparNagelScoringParameters implements CharyparNagelScoringParametersForPerson {
	private final PlanCalcScoreConfigGroup config;
	private final ScenarioConfigGroup scConfig;
	private CharyparNagelScoringParameters params = null;

	public UniformCharyparNagelScoringParameters(PlanCalcScoreConfigGroup config, ScenarioConfigGroup scConfig) {
		this.config = config;
		this.scConfig = scConfig;
	}

	@Override
	public CharyparNagelScoringParameters getScoringParameters(Person person) {
		if (this.params == null) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
* end up with different params-object, although all objects will have the same
* values in them due to using the same config. Still much better from a memory performance
* point of view than giving each ScoringFunction its own copy of the params.
*/
			this.params = CharyparNagelScoringParameters.getBuilder(
					this.config,
					this.config.getScoringParameters(null),
					scConfig).create();
		}

		return this.params;
	}
}
