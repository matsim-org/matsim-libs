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

package org.matsim.core.scoring.functions;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author rashid_waraich
 */
public final class CharyparNagelAgentStuckScoring implements org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring {

	private double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	private final ScoringParameters params;

	public CharyparNagelAgentStuckScoring(final ScoringParameters params) {
		this.params = params;
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

	@Override
	public void explainScore(StringBuilder out) {
		out.append("agentStuck_util=").append(this.score);
	}
}
