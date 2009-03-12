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

import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;

public class PlanAverageScore extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private double sumScores = 0.0;
	private long cntScores = 0;
	
	public PlanAverageScore() {
		super();
	}
	
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

