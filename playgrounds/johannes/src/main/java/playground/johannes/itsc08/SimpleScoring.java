/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleScoring.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.itsc08;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.ScoringFunction;

/**
 * @author illenberger
 *
 */
public class SimpleScoring implements ScoringFunction {
	
	public static double lateTime;

	public static double lateCount;
	
	public static double earlyTime;
	
	public static double earlyCount;

	public static final double beta_travel = -6;
	
	public static final double beta_late = -18;
	
	private LegImpl currentLeg;
	
	private double score;
	
	private double startTime;

	public void addMoney(double amount) {
	}

	public void agentStuck(double time) {
	}

	public void endActivity(double time) {
	}

	public void endLeg(double time) {
		if(time > currentLeg.getArrivalTime()) {
			score += (currentLeg.getArrivalTime() - startTime) * beta_travel / 3600.0;
			score += (time - currentLeg.getArrivalTime()) * beta_late / 3600.0;
			lateTime += (time - currentLeg.getArrivalTime());
			lateCount++;
		} else {
			score += (time - startTime) * beta_travel;
			earlyTime += (currentLeg.getArrivalTime() - time);
			earlyCount++;
		}
		currentLeg = null;
		startTime = Double.NaN;
	}

	public void finish() {
	
	}

	public double getScore() {
		return score;
	}

	public void reset() {
	
		score = 0;
	}

	public void startActivity(double time, Activity act) {
	}

	public void startLeg(double time, Leg leg) {
		currentLeg = (LegImpl) leg;
		startTime = time;
	}

}
