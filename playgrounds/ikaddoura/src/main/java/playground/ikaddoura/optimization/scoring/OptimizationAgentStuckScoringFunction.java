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

import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class OptimizationAgentStuckScoringFunction extends CharyparNagelAgentStuckScoring {
	
	private final double agentStuckScore;
	
	public OptimizationAgentStuckScoringFunction(CharyparNagelScoringParameters params, double agentStuckScore) {
		super(params);
		this.agentStuckScore = agentStuckScore;
	}

	@Override
	public void agentStuck(final double time) {
		this.score += this.agentStuckScore;
	}
	
}
