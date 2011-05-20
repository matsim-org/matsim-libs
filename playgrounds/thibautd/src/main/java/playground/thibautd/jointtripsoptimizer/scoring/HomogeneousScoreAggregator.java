/* *********************************************************************** *
 * project: org.matsim.*
 * HomogeneousScoreAggregator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.scoring;

import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;

/**
 * A simple {@link ScoresAggregator} which simply returns the sum of the
 * individual scores.
 *
 * @author thibautd
 */
public class HomogeneousScoreAggregator implements ScoresAggregator {

	private final Collection<? extends Plan> individualPlans;

	public HomogeneousScoreAggregator(final Collection<? extends Plan> individualPlans) {
		this.individualPlans = individualPlans;
	}

	@Override
	public Double getJointScore() {
		double score = 0d;

		for (Plan plan : this.individualPlans) {
			try {
				score += plan.getScore();
			} catch (NullPointerException e) {
				// at least one of the individuals is "unscored"
				return null;
			}
		}

		return score;
	}
}

