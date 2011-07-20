/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityScorer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.interaction;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.interfaces.BasicScoring;

/**
 * @author illenberger
 *
 */
public class JointActivityScorer implements BasicScoring {

	public static double totalJoinTime = 0;
	
	public static int jointAgents = 0;
	
	public final double beta_join;
	
	private Person ego;
	
	private Person alter;
	
	private VisitorTracker tracker;
	
	private double score;
	
	public JointActivityScorer(Person ego, Person alter, VisitorTracker tracker, double beta) {
		this.ego = ego;
		this.alter = alter;
		this.tracker = tracker;
		this.beta_join = beta;
	}
	
	@Override
	public void finish() {
		score = 0.0;
		double time = tracker.timeOverlap(ego, alter);
		score = beta_join * time;
		
		if(time > 0)
			jointAgents++;
		
		totalJoinTime += time;
	}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void reset() {
		
	}

}
