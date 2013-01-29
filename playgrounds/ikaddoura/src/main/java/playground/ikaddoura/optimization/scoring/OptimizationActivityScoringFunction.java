/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.ikaddoura.optimization.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.PtConstants;

public class OptimizationActivityScoringFunction extends CharyparNagelActivityScoring {

	public OptimizationActivityScoringFunction(CharyparNagelScoringParameters params) {
		super(params);
	}

	@Override
	protected double calcActScore(double arrivalTime, double departureTime, Activity act) {
		if (act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			return 0.0;
		} else {
			return super.calcActScore(arrivalTime, departureTime, act);
		}
	}

}

