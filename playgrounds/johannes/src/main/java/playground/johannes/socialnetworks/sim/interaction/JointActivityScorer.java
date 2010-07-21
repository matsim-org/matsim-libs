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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.interfaces.BasicScoring;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class JointActivityScorer implements BasicScoring {

	public static double totalJoinTime = 0;
	
//	private final static Logger logger = Logger.getLogger(JointActivityScorer.class);
	
	private final static double beta_join = 100/3600.0;
	
	private Person person;
	
	private VisitorTracker tracker;
	
	private Set<Person> friends;
	
	private double score;
	
//	private static int joins;
	
	public JointActivityScorer(Person person, VisitorTracker tracker, Map<Person, SocialVertex> vertexMapping) {
		this.person = person;
		this.tracker = tracker;
		
		SocialVertex vertex = vertexMapping.get(person);
		friends = new HashSet<Person>();
		for(SocialVertex neighbor : vertex.getNeighbours()) {
			friends.add(neighbor.getPerson().getPerson());
		}
	}
	
	@Override
	public void finish() {
		score = 0.0;
		double time = tracker.timeOverlap(person, friends);
		score = beta_join * time;
		
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
