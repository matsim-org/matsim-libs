/* *********************************************************************** *
 * project: org.matsim.*
 * PieceWiseLinearFare.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.scoring;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;

/**
 * @author thibautd
 */
public class StepBasedFare implements LegScoring {
	private final PlanCalcScoreConfigGroup scoringGroup;
	private final StepBasedFareConfigGroup fareGroup;
	private double score = 0;

	public StepBasedFare(
			final PlanCalcScoreConfigGroup scoringGroup,
			final StepBasedFareConfigGroup fareGroup) {
		this.scoringGroup = scoringGroup;
		this.fareGroup = fareGroup;
	}

	@Override
	public void finish() { }

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void handleLeg(final Leg leg) {
		final double travelTime = leg.getTravelTime();
		score -= calcPrice( travelTime );
	}

	private double calcPrice( final double travelTime ) {
		if ( travelTime > fareGroup.getMaxTime_sec() ) {
			return fareGroup.getOvertimePenalty() * scoringGroup.getMarginalUtilityOfMoney();
		}

		final int stepNr = (int) (travelTime / fareGroup.getStepDuration_sec());
		return stepNr * fareGroup.getStepPrice() * scoringGroup.getMarginalUtilityOfMoney();
	}
}

