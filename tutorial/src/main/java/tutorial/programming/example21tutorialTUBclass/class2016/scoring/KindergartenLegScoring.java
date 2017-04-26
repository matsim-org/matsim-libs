/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.programming.example21tutorialTUBclass.class2016.scoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

public class KindergartenLegScoring extends CharyparNagelLegScoring {

	public KindergartenLegScoring(ScoringParameters params, Network network) {
		super(params, network);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double legScore = super.calcLegScore(departureTime, arrivalTime, leg);
		return legScore;
	}

	@Override
	public void handleLeg(Leg leg) {
		double legScore = calcLegScore(leg.getDepartureTime(), leg.getDepartureTime() + leg.getTravelTime(), leg);
		this.score += legScore;

	}

}
