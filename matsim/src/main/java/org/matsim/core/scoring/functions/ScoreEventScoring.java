/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

import org.matsim.core.scoring.SumScoringFunction;

/**
 * @author mrieser / Simunto
 */
public final class ScoreEventScoring implements SumScoringFunction.ScoreScoring {

	private double score = 0.0;

	@Override
	public void addScore(final double amount) {
		this.score += amount;
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return this.score;
	}

	@Override
	public void explainScore(StringBuilder out) {
		out.append("scoreEvents_util=").append(score);
	}
}
