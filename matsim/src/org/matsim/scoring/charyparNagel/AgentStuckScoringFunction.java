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

package org.matsim.scoring.charyparNagel;

import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.interfaces.AgentStuckScoring;
import org.matsim.scoring.interfaces.BasicScoring;


public class AgentStuckScoringFunction implements AgentStuckScoring, BasicScoring {

	protected double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public AgentStuckScoringFunction(final CharyparNagelScoringParameters params) {
		this.params = params;
		this.reset();
	}

	public void reset() {
		this.score = INITIAL_SCORE;
	}

	public void agentStuck(final double time) {

		this.score += getStuckPenalty();
	}

	public void finish() {

	}

	public double getScore() {
		return this.score;
	}

	private double getStuckPenalty() {
		return this.params.abortedPlanScore;
	}

}
