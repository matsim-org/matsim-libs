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

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class ActivityOvertimeEvaluator implements Evaluator {

	private final static String HOME = "home";
	
	private final double beta;
	
	private final Map<String, Map<Person, Double>> desiredDurations;
	
	public ActivityOvertimeEvaluator(double beta, Map<String, Map<Person, Double>> desiredDurations) {
		this.beta = beta;
		this.desiredDurations = desiredDurations;
	}
	
	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;
		for(int i = 0; i < trajectory.getElements().size(); i += 2) {
			Activity act = (Activity)trajectory.getElements().get(i);
			
			if(!act.getType().equals(HOME)) {
				double duration = trajectory.getTransitions().get(i+1) - trajectory.getTransitions().get(i);
				double desiredDuration = desiredDurations.get(act.getType()).get(trajectory.getPerson());
				
				double delta = duration / desiredDuration;
				if(delta > 1) {
					score -= Math.exp(beta * delta);
				}
			}
		}
		
		return score;
	}

}
