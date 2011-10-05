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

import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.population.Desires;

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class ActivityEvaluator implements Evaluator {

	private final static String HOME = "home";
	
	private final static double SCALE = -1.0;
	
	private final double beta;
	
	private final Map<Person, Desires> desires;
	
	private final Map<String, Double> priorities;
	
	public ActivityEvaluator(double beta, Map<Person, Desires> desires, Map<String, Double> priorities) {
		this.beta = beta;
		this.desires = desires;
		this.priorities = priorities;
	}
	
	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;
		for(int i = 0; i < trajectory.getElements().size(); i += 2) {
			Activity act =  (Activity) trajectory.getElements().get(i);
			
			double t = trajectory.getTransitions().get(i+1) - trajectory.getTransitions().get(i);
			
			double t_star = Math.max(t, 3600);
			if(!act.getType().equals(HOME)) {
				t_star = desires.get(trajectory.getPerson()).getActivityDuration(act.getType());
			}
			
			double priority = getPriority(act.getType());
			
			double t_zero = t_star * Math.exp(SCALE/(t_star * priority * beta));
			if(t_zero == 0)
				throw new IllegalArgumentException("t_zero must not be 0!");
//			t_zero = Math.max(t_zero, 1.0);

			score += beta * t_star * Math.log(t/t_zero);
		}
		
		return score;
	}
	
	private double getPriority(String type) {
		return priorities.get(type);
	}

}
