/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceVisitorsTask.java
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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.IOException;
import java.util.*;

/**
 * @author illenberger
 * 
 */
public class DistanceVisitorsTask extends TrajectoryAnalyzerTask {

	private final VisitorTracker tracker;

	private final SocialGraph graph;

	private final Map<Person, SocialVertex> mapping;

	private final ActivityFacilities facilities;

	public DistanceVisitorsTask(VisitorTracker tracker, SocialGraph graph, ActivityFacilities facilities) {
		this.tracker = tracker;
		this.graph = graph;
		this.facilities = facilities;

		mapping = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		for (SocialVertex v : graph.getVertices()) {
			mapping.put(v.getPerson().getPerson(), v);
		}
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<Trajectory> values = new TripDistanceMean(null, facilities, CartesianDistanceCalculator.getInstance()).values(trajectories);

		Set<String> purposes = new HashSet<String>();
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity) t.getElements().get(i)).getType());
			}
		}
		
		purposes.add(null);
		for(String purpose : purposes) {
			TDoubleDoubleHashMap correl = calcCorrelation(trajectories, values, purpose);
			try {
				if(purpose == null)
					purpose = "all";
				StatsWriter.writeHistogram(correl, "visitors", "d", String.format("%1$s/d_visitors.%2$s.txt", getOutputDirectory(), purpose));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}

	private TDoubleDoubleHashMap calcCorrelation(Set<Trajectory> trajectories, TObjectDoubleHashMap<Trajectory> distances, String type) {
		TDoubleArrayList distVals = new TDoubleArrayList(trajectories.size());
		TDoubleArrayList visitorVals = new TDoubleArrayList(trajectories.size());

		for (Trajectory t : trajectories) {
			Person p = t.getPerson();
			Activity act = (Activity) t.getElements().get(2);
			if (type == null || act.getType().equals(type)) {
				SocialVertex v = mapping.get(p);
				List<Person> alters = new ArrayList<Person>(v.getNeighbours().size());
				for (SocialVertex w : v.getNeighbours())
					alters.add(w.getPerson().getPerson());

				int visitors = tracker.metAlters(t.getPerson(), alters);
				double d = distances.get(t);
				if (d > 0) {
					distVals.add(d);
					visitorVals.add(visitors);
				}
			}
		}

		return Correlations.mean(visitorVals.toNativeArray(), distVals.toNativeArray());
	}
}
