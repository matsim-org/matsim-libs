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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.stats.DummyDiscretizer;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

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
		DescriptiveStatistics timeStatsAll = new DescriptiveStatistics();
		DescriptiveStatistics visitorStatsAll = new DescriptiveStatistics();
		Map<String, DescriptiveStatistics> timeStatsTypeMap = new HashMap<String, DescriptiveStatistics>();
		Map<String, DescriptiveStatistics> visitorStatsTypeMap = new HashMap<String, DescriptiveStatistics>();
		
		for (Trajectory t : trajectories) {
			Person person = t.getPerson();
			SocialVertex ego = personVertexMap.get(person);
			String type = ((Activity) t.getElements().get(2)).getType();
			
			DescriptiveStatistics timeStatsType = timeStatsTypeMap.get(type);
			DescriptiveStatistics visitorStatsType = visitorStatsTypeMap.get(type);
			
			if(timeStatsType == null) {
				timeStatsType = new DescriptiveStatistics();
				timeStatsTypeMap.put(type, timeStatsType);
				visitorStatsType = new DescriptiveStatistics();
				visitorStatsTypeMap.put(type, visitorStatsType);
			}
			
			int visitors = 0;
			List<Person> alterPersons = new ArrayList<Person>(ego.getNeighbours().size());
			for (SocialVertex alter : ego.getNeighbours()) {
				double time = tracker.timeOverlap(person, alter.getPerson().getPerson());
				if (time > 0) {
					timeStatsType.addValue(time);
					timeStatsAll.addValue(time);
				}
				
				alterPersons.add(alter.getPerson().getPerson());
			}
			
			visitors = tracker.metAlters(person, alterPersons);
			visitorStatsType.addValue(visitors);
			visitorStatsAll.addValue(visitors);
		}

		String timeKey = "t_joint";
		results.put(timeKey, timeStatsAll);
		for(Entry<String, DescriptiveStatistics> entry : timeStatsTypeMap.entrySet()) {
			results.put("t_joint_" + entry.getKey(), entry.getValue());
		}
		
		String visitorKey = "visitors";
		results.put(visitorKey, visitorStatsAll);
		for(Entry<String, DescriptiveStatistics> entry : visitorStatsTypeMap.entrySet()) {
			results.put("visitors_" + entry.getKey(), entry.getValue());
		}
		
		if (outputDirectoryNotNull()) {
			try {
				writeHistograms(timeStatsAll, timeKey, 50, 50);
				writeHistograms(visitorStatsAll, new DummyDiscretizer(), visitorKey, false);
				
				for(Entry<String, DescriptiveStatistics> entry : timeStatsTypeMap.entrySet()) {
					writeHistograms(entry.getValue(), "t_joint_" + entry.getKey(), 50, 50);
				}
				
				for(Entry<String, DescriptiveStatistics> entry : visitorStatsTypeMap.entrySet()) {
					writeHistograms(entry.getValue(), new DummyDiscretizer(), "visitors_" + entry.getKey(), false);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
