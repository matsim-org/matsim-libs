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
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.core.scoring.interfaces.BasicScoring;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class JointActivityScorer implements BasicScoring {

	public static double totalJoinTime = 0;
	
	public static int jointAgents = 0;
	
	public final double beta_join;
	
	private SocialVertex ego;
	
	private SocialVertex alter;
	
	private Person egoPerson;
	
	private Person alterPerson;
	
	private VisitorTracker tracker;
	
	private double score;
	
	private static final DistanceCalculator calc = new CartesianDistanceCalculator();
	
	private static final Discretizer discretizer = new LinearDiscretizer(1000.0); 
	
	public JointActivityScorer(SocialVertex ego, SocialVertex alter, VisitorTracker tracker, double beta) {
		this.ego = ego;
		this.alter = alter;
		this.egoPerson = ego.getPerson().getPerson();
		this.alterPerson = alter.getPerson().getPerson();
		this.tracker = tracker;
		this.beta_join = beta;
	}
	
	@Override
	public void finish() {
		score = 0.0;
		double time = tracker.timeOverlap(egoPerson, alterPerson);
		double d = calc.distance(ego.getPoint(), alter.getPoint());
		d = discretizer.index(d);
		
		score = beta_join * d * time;
		
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
