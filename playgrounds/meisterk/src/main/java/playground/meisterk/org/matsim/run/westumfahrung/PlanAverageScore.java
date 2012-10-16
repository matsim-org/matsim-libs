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

package playground.meisterk.org.matsim.run.westumfahrung;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PlanAverageScore extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private double sumScores = 0.0;
	private long cntScores = 0;
	
	public PlanAverageScore() {
		super();
	}
	
	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			if (plan.isSelected()) {
				run(plan);
			}
		}
	}
	
	@Override
	public final void run(Plan plan) {
		Double score = plan.getScore();

		if ((score != null) && (!score.isInfinite()) && (!score.isNaN())) {
			sumScores += score.doubleValue();
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

