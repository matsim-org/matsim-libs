
/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTypeOpeningIntervalCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.misc.OptionalTime;

public class ActivityTypeOpeningIntervalCalculator implements OpeningIntervalCalculator {
	private final ScoringParameters params;

	public ActivityTypeOpeningIntervalCalculator(ScoringParameters params) {
		this.params = params;
	}

	@Override
	public OptionalTime[] getOpeningInterval(final Activity act) {

		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"planCalcScore\" in the config file).");
		}

		OptionalTime openingTime = actParams.getOpeningTime();
		OptionalTime closingTime = actParams.getClosingTime();

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time

		return new OptionalTime[]{openingTime, closingTime};
	}
}
