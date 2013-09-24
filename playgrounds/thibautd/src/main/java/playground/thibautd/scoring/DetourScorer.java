/* *********************************************************************** *
 * project: org.matsim.*
 * DetourScorer.java
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
package playground.thibautd.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ActivityScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;

import playground.thibautd.socnetsim.population.JointActingTypes;

/**
 * ONLY WORKS FOR CAR_PU_JOINT_DO_CAR TRIPS!
 * @author thibautd
 */
public class DetourScorer implements ActivityScoring , LegScoring {
	private final double marginalUtilityOfDetour_s;

	// to be sure that reset resets everything consistently
	private static class MutableFields {
		private Leg lastLeg = null;
		private double accumulatedScore = 0;
		private boolean lastActivityWasDropOff = false;
	}

	private MutableFields fields = new MutableFields();

	public DetourScorer(
			final double marginalUtilityOfDetour_s ) {
		this.marginalUtilityOfDetour_s = marginalUtilityOfDetour_s;
	}

	@Override
	public void finish() {}

	@Override
	public double getScore() {
		return fields.accumulatedScore;
	}

	@Override
	public void reset() {
		this.fields = new MutableFields();
	}

	@Override
	public void startLeg(
			final double time,
			final Leg leg) {
		fields.lastLeg = leg;

		if ( fields.lastActivityWasDropOff ) {
			fields.accumulatedScore += marginalUtilityOfDetour_s * leg.getTravelTime();
		}
	}

	@Override
	public void endLeg(final double time) {}

	@Override
	public void startActivity(
			final double time,
			final Activity act) {
		fields.lastActivityWasDropOff = act.getType().equals( JointActingTypes.DROP_OFF );

		if ( act.getType().equals( JointActingTypes.PICK_UP ) ) {
			fields.accumulatedScore += marginalUtilityOfDetour_s * fields.lastLeg.getTravelTime();
		}
	}

	@Override
	public void endActivity(final double time, final Activity act) {
		fields.lastActivityWasDropOff = act.getType().equals( JointActingTypes.DROP_OFF );
	}
}

