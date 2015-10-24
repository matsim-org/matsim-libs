/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTypeEvaluator.java
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
package playground.johannes.coopsim.eval;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;

/**
 * @author illenberger
 * 
 */
public class ActivityTypeEvaluator implements Evaluator {

	private final double delta;

	private final Map<Person, ActivityDesires> desires;

	private static boolean isLogging;

	private static DescriptiveStatistics stats;

	public ActivityTypeEvaluator(double delta, Map<Person, ActivityDesires> desires) {
		this.delta = delta;
		this.desires = desires;
	}

	@Override
	public double evaluate(Trajectory trajectory) {
		Activity act = (Activity) trajectory.getElements().get(2);
		String type = act.getType();
		String desiredType = desires.get(trajectory.getPerson()).getActivityType();

		double score = 0;
		if (!type.equals(desiredType)) {
			score = delta;
		}

		if (isLogging)
			stats.addValue(score);

		return score;
	}

	public static void startLogging() {
		stats = new DescriptiveStatistics();
		isLogging = true;
	}

	public static DescriptiveStatistics stopLogging() {
		isLogging = false;
		return stats;
	}
}
