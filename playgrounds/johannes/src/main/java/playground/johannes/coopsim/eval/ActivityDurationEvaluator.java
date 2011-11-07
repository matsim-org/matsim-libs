/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityOvertimeEvaluator.java
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

import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class ActivityDurationEvaluator implements Evaluator {

	private final static String HOME = "home";
	
	private final double beta;
	
	private final Map<Person, ActivityDesires> desires;
	
	private static boolean isLogging;
	
	private static DescriptiveStatistics stats;
	
	public ActivityDurationEvaluator(double beta, Map<Person, ActivityDesires> desires) {
		this.beta = beta;
		this.desires = desires;
	}
	
	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;
		for(int i = 0; i < trajectory.getElements().size(); i += 2) {
			Activity act = (Activity)trajectory.getElements().get(i);
			
			if(!act.getType().equals(HOME) && !act.getType().equals("idle")) {
				double duration = trajectory.getTransitions().get(i+1) - trajectory.getTransitions().get(i);
				double desiredDuration = desires.get(trajectory.getPerson()).getActivityDuration(act.getType());
				
				double frac = duration / desiredDuration;
				if(frac > 1) {
					score -= beta * Math.exp(frac - 1);
				} else if (frac < 1) {
					score -= beta * Math.exp(1/frac - 1);
				}
			}
		}
		
		if(isLogging)
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
