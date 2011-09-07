/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityTask.java
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.math.DummyDiscretizer;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 * 
 */
public class JointActivityTask extends TrajectoryAnalyzerTask {

	private final VisitorTracker tracker;

	private final Map<Person, SocialVertex> personVertexMap;

	public JointActivityTask(SocialGraph graph, VisitorTracker tracker) {
		this.tracker = tracker;
		
		personVertexMap = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		for(SocialVertex v : graph.getVertices()) {
			personVertexMap.put(v.getPerson().getPerson(), v);
		}
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics timeStats = new DescriptiveStatistics();
		DescriptiveStatistics visitorStats = new DescriptiveStatistics();

		for (Trajectory t : trajectories) {
			Person person = t.getPerson();
			SocialVertex ego = personVertexMap.get(person);

			int visitors = 0;
			for (SocialVertex alter : ego.getNeighbours()) {
				double time = tracker.timeOverlap(person, alter.getPerson().getPerson());
				if (time > 0) {
					timeStats.addValue(time);
					visitors++;
				}
			}

			visitorStats.addValue(visitors);
		}

		String timeKey = "t_joint";
		results.put(timeKey, timeStats);

		String visitorKey = "visitors";
		results.put(visitorKey, visitorStats);

		if (outputDirectoryNotNull()) {
			try {
				writeHistograms(timeStats, timeKey, 50, 50);
				writeHistograms(visitorStats, new DummyDiscretizer(), visitorKey, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
