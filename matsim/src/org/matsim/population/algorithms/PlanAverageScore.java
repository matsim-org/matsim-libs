/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAverageScore.java
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

package org.matsim.population.algorithms;

import java.util.Iterator;

import org.matsim.population.Person;
import org.matsim.population.Plan;

public class PlanAverageScore extends AbstractPersonAlgorithm implements PlanAlgorithmI {

	private double sumScores = 0.0;
	private long cntScores = 0;
	private double limit = Double.NEGATIVE_INFINITY;
	
	public PlanAverageScore() {
		super();
	}
	
	/**
	 * Constructs a PlanAverageScorer when some plans with a score lower than 
	 * limit are not used to calculate the average.
	 * 
	 * @param limit only scores equal or bigger than limit will be average
	 */
	public PlanAverageScore(double limit) {
		super();
		this.limit = limit;
	}

	// TODO PlanCalcScore should NOT inherit from AbstractPersonAlgorithm, because it is a PlanAlgorithm
	// this functionality is only provided a.t.m. until the complete PlanAlgorithm-API is ready.
	@Override
	public void run(Person person) {
		Iterator<Plan> iter = person.getPlans().iterator();
		while (iter.hasNext()) {
			Plan plan = iter.next();
			if (plan.isSelected()) {
				run(plan);
			}
		}
	}
	
	public final void run(Plan plan) {
		double score = plan.getScore();

		if (!(Double.isInfinite(score) || Double.isNaN(score) || (score < limit))) {
			sumScores += score;
			cntScores++;
		}
	}
	
	public final double getAverage() {
		return (sumScores / cntScores);
	}
	
	public final long getCount() {
		return cntScores;
	}
	
}

