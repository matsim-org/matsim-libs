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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.population.Desires;

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class ActivityDurationEvaluator implements Evaluator {

	private final static String HOME = "home";
	
	private final double beta;
	
	private final Map<Person, Desires> desires;
	
	public ActivityDurationEvaluator(double beta, Map<Person, Desires> desires) {
		this.beta = beta;
		this.desires = desires;
	}
	
	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;
		for(int i = 0; i < trajectory.getElements().size(); i += 2) {
			Activity act = (Activity)trajectory.getElements().get(i);
			
			if(!act.getType().equals(HOME)) {
				double duration = trajectory.getTransitions().get(i+1) - trajectory.getTransitions().get(i);
				double desiredDuration = desires.get(trajectory.getPerson()).getActivityDuration(act.getType());
				
				double frac = duration / desiredDuration;
				if(frac > 1) {
					score -= Math.exp(beta * (frac - 1));
				} else if (frac < 1) {
					score -= Math.exp(beta * (1/frac - 1));
				}
			}
		}
		
		return score;
	}

}
