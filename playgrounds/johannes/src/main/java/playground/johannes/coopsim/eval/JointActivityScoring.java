/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityScoring.java
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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.interfaces.BasicScoring;

import playground.johannes.socialnetworks.graph.social.SocialVertex;


/**
 * @author illenberger
 *
 */
public class JointActivityScoring implements BasicScoring {

	private final SocialVertex ego;
	
	private final Set<Person> alters;
	
	private final VisitorTracker tracker;
	
	private final double beta;
	
	private double score;
	
	public JointActivityScoring(SocialVertex ego, VisitorTracker tracker, double beta) {
		this.ego = ego;
		this.tracker = tracker;
		this.beta = beta;
		
		alters = new HashSet<Person>();
		for(SocialVertex alter : ego.getNeighbours())
			alters.add(alter.getPerson().getPerson());
	}
	
	@Override
	public void finish() {
		double time = tracker.timeOverlap(ego.getPerson().getPerson(), alters);
		score = time * beta;
	}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void reset() {
	}

}
