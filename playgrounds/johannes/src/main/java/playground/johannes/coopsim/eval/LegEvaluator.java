/* *********************************************************************** *
 * project: org.matsim.*
 * LegEvaluator.java
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
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class LegEvaluator implements Evaluator {

	private final double beta;
	
	private static boolean isLogging;
	
	private static DescriptiveStatistics stats;
	
	public LegEvaluator(double beta) {
		this.beta = beta;
	}
	
	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;
		for(int i = 1; i < trajectory.getElements().size(); i+=2) {
			double t = trajectory.getTransitions().get(i+1) - trajectory.getTransitions().get(i);
			score += beta * t;
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
