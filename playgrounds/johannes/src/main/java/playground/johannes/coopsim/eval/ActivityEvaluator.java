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
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;

/**
 * @author illenberger
 * 
 */
public class ActivityEvaluator implements Evaluator {

	private final static Logger logger = Logger.getLogger(ActivityEvaluator.class);
	
	private static int tZeroCount;
	
	private final static String HOME = "home";
	
	private final static String IDLE = "idle";

	private final static double IDLE_PENALTY = -1E6;
	
	private final static double SCALE = -1.0;

	private final double beta;

	private final Map<Person, ActivityDesires> desires;

	private final Map<String, Double> priorities;

	private static boolean isLogging;
	
	private static DescriptiveStatistics stats;
	
	public ActivityEvaluator(double beta, Map<Person, ActivityDesires> desires, Map<String, Double> priorities) {
		this.beta = beta;
		this.desires = desires;
		this.priorities = priorities;
	}

	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;
		for (int i = 0; i < trajectory.getElements().size(); i += 2) {
			Activity act = (Activity) trajectory.getElements().get(i);

			if (act.getType().equals(IDLE)) {
				score += IDLE_PENALTY;
			} else {// if(!act.getType().equals(HOME)) {
				double t = trajectory.getTransitions().get(i + 1) - trajectory.getTransitions().get(i);

				ActivityDesires desire = desires.get(trajectory.getPerson());
				double t_star = desire.getActivityDuration(act.getType());
				
				if (act.getType().equals(HOME)) {
					String lType = ((Activity) trajectory.getElements().get(2)).getType();
					if (lType.equals(IDLE))
						t_star = 14400; // arbitrary
					else {
						double t_start = desire.getActivityStartTime(lType);
						double t_dur = desire.getActivityDuration(lType);
								
						if (i == 0) {
							t_star = t_start;
						} else if(i == 4) {
							t_star = 86400 - (t_start + t_dur);
						} else {
							throw new RuntimeException("Not a starting or ending home activity!");
						}
						
						t_star = Math.max(t_star, 3600);
					}
				}
				
				double priority = getPriority(act.getType());

//				double t_zero = t_star * Math.exp(SCALE / (t_star * priority * beta));
				double t_zero = t_star * Math.exp(SCALE / (t_star * priority));
				if (t_zero < 0.0001) {
					t_zero = 0.0001;
					tZeroCount++;
					if(tZeroCount % 10000 == 0)
						logger.warn("10000 repeated warnings: t_zero<0.0001, setting t_zero=0.0001. index = " + i);
				}

				t = Math.max(t, 2.0);
				score += beta * t_star * Math.log(t / t_zero);
			}
		}

		
		
		if(isLogging)
			stats.addValue(score);
		
		return score;
	}

	private double getPriority(String type) {
		return priorities.get(type);
	}

	public static void startLogging() {
		ActivityEvaluator2.startLogging();
//		stats = new DescriptiveStatistics();
//		isLogging = true;
	}
	
	public static DescriptiveStatistics stopLogging() {
		return ActivityEvaluator2.stopLogging();
//		isLogging = false;
//		return stats;
	}
}
