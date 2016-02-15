/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEvaluator.java
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
public class ActivityEvaluator2 implements Evaluator {

//	private final static Logger logger = Logger.getLogger(ActivityEvaluator2.class);

	private final static String HOME = "home";

	private final static String IDLE = "idle";

	private final static double IDLE_PENALTY = -1E6;

	private final double beta;

	private final Map<Person, ActivityDesires> desires;

//	private final Map<String, Double> priorities;

	private static boolean isLogging;

	private static DescriptiveStatistics stats;

	public ActivityEvaluator2(double beta, Map<Person, ActivityDesires> desires, Map<String, Double> priorities) {
		this.beta = beta;
		this.desires = desires;
//		this.priorities = priorities;
	}

	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;
//		double t_sum = 0;
		double t_home = 0;
		double t_star_sum = 0;

		for (int i = 0; i < trajectory.getElements().size(); i += 2) {
			Activity act = (Activity) trajectory.getElements().get(i);

			double t = trajectory.getTransitions().get(i + 1) - trajectory.getTransitions().get(i);
			
			if (act.getType().equals(IDLE)) {
				score += IDLE_PENALTY;
//				t_sum += t;
				t_star_sum += t;
			} else if(act.getType().equals(HOME)) {
				t_home += t;
			} else {
//				t_sum += t;
				
				double v_star = getPriority(act.getType());

				if (beta == 0) {
					score += v_star;
				} else {

					ActivityDesires desire = desires.get(trajectory.getPerson());
					double t_star = desire.getActivityDuration(act.getType());

					score += calcScore(t, t_star, v_star);

					t_star_sum += t_star;
				}
			}
		}

		if (beta == 0) {
			score += getPriority(HOME);
		} else {
			double t_end = trajectory.getTransitions().get(trajectory.getTransitions().size() - 1);
			double t_star_home = t_end - t_star_sum;
			if (t_home <= 0 || t_star_home <= 0) {
				throw new RuntimeException(String.format("t_home=%1$s, t_start_home=%2$s.", t_home, t_star_home));
			}

			score += calcScore(t_home, t_star_home, getPriority(HOME));
		}
		if (isLogging)
			stats.addValue(score);

		return score;
	}

	private double calcScore(double t, double t_star, double v_star) {
		double beta_h = beta * 3600.0;
		double t_star_h = t_star / 3600.0;
		double t_h = t / 3600.0;
		return beta_h * t_star_h * Math.log(t_h / t_star_h * Math.exp(v_star / (beta_h * t_star_h)));
	}

	private double getPriority(String type) {
		return 1;
//		return priorities.get(type);
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
