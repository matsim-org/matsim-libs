/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityEvaluator.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class JointActivityEvaluator implements Evaluator {

	private final double beta;
	
	private final VisitorTracker tracker;
	
	private final Map<Person, SocialVertex> vertices;
	
	private final Map<Person, List<Person>> alters;
	
	public JointActivityEvaluator(double beta, VisitorTracker tracker, SocialGraph graph) {
		this.beta = beta;
		this.tracker = tracker;
		
		vertices = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		alters = new HashMap<Person, List<Person>>(graph.getVertices().size());
		for(SocialVertex v : graph.getVertices()) {
			vertices.put(v.getPerson().getPerson(), v);
			List<Person> neighbours = new ArrayList<Person>(v.getNeighbours().size());
			for(SocialVertex alter : v.getNeighbours()) {
				neighbours.add(alter.getPerson().getPerson());
			}
			alters.put(v.getPerson().getPerson(), neighbours);
		}
	}
	
	@Override
	public double evaluate(Trajectory trajectory) {
		double time = 0;
//		SocialVertex ego = vertices.get(trajectory.getPerson());
		
//		for(SocialVertex alter : ego.getNeighbours()) {
//			time += tracker.timeOverlap(trajectory.getPerson(), alter.getPerson().getPerson());
//		}
		time = tracker.timeOverlap(trajectory.getPerson(), alters.get(trajectory.getPerson()));
		return time * beta;
	}

}
