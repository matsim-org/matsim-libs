/* *********************************************************************** *
 * project: org.matsim.*
 * DenivelationAlongRouteScoring.java
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
package playground.thibautd.router.multimodal;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

/**
 * @author thibautd
 */
public class DenivelationAlongRouteScoring implements BasicScoring {
	private final LinkSlopeScorer scorer;

	private final Plan plan;
	private final String mode;
	private double score = Double.NaN;

	public DenivelationAlongRouteScoring(
			final LinkSlopeScorer scorer,
			final Plan plan,
			final String mode) {
		this.scorer = scorer;
		this.plan = plan;
		this.mode = mode;
	}

	@Override
	public void finish() {}

	@Override
	public double getScore() {
		if ( !Double.isNaN( score ) ) return score;

		score = 0;
		for ( Leg l : TripStructureUtils.getLegs( plan ) ) {
			if ( !l.getMode().equals( mode ) ) continue;
			final NetworkRoute route = (NetworkRoute) l.getRoute();

			score += scorer.calcGainUtil( route );
		}

		return score;
	}
}

