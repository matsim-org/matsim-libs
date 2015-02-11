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

package playground.artemc.heterogeneity.scoring.functions;

import org.matsim.core.scoring.ScoringFunctionAccumulator.AgentStuckScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author rashid_waraich
 */
public class CharyparNagelAgentStuckScoring implements AgentStuckScoring, BasicScoring, org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring {

	protected double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final PersonalScoringParameters params;

	public CharyparNagelAgentStuckScoring(final PersonalScoringParameters params) {
		this.params = params;
		this.reset();
	}

	@Override
	public void reset() {
		this.score = INITIAL_SCORE;
	}

	@Override
	public void agentStuck(final double time) {

		this.score += getStuckPenalty();
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

	private double getStuckPenalty() {
		return this.params.abortedPlanScore;
	}

}
