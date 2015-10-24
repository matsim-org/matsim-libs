/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityGroupMembers.java
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
package playground.johannes.coopsim.analysis;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialVertex;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;

import java.util.*;

/**
 * @author illenberger
 *
 */
public class ActivityGroupMembers extends AbstractTrajectoryProperty {

	private final VisitorTracker tracker;
	
	private final Map<Person, SocialVertex> vertices;
	
	public ActivityGroupMembers(VisitorTracker tracker, SocialGraph graph) {
		this.tracker = tracker;
	
		vertices = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		for (SocialVertex v : graph.getVertices()) {
			vertices.put(v.getPerson().getPerson(), v);
		}
	}
	
	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> members = new TObjectDoubleHashMap<Trajectory>(trajectories.size());
		
		for(Trajectory t : trajectories) {
			SocialVertex v = vertices.get(t.getPerson());
			List<Person> alters = new ArrayList<Person>(v.getNeighbours().size());
			for (SocialVertex w : v.getNeighbours())
				alters.add(w.getPerson().getPerson());

			members.put(t, tracker.metAlters(t.getPerson(), alters));
		}
		
		return members;
	}

}
