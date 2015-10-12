/* *********************************************************************** *
 * project: org.matsim.*
 * SocialTripCorrelationTask.java
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
import gnu.trove.TDoubleObjectHashMap;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.stats.DummyDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.IOException;
import java.util.*;

/**
 * @author illenberger
 *
 */
public abstract class SocialTripCorrelationTask extends TrajectoryAnalyzerTask {

	private final VisitorTracker tracker;
	
	private Map<Person, SocialVertex> vertexMapping;
	
	private final String key;
	
	public SocialTripCorrelationTask(SocialGraph graph, VisitorTracker tracker, String key) {
		this.key = key;
		this.tracker = tracker;
		
		vertexMapping = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		for(SocialVertex v : graph.getVertices())
			vertexMapping.put(v.getPerson().getPerson(), v);
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TDoubleArrayList xvals = new TDoubleArrayList(trajectories.size());
		TDoubleArrayList yvals = new TDoubleArrayList(trajectories.size());
		
		for(Trajectory t : trajectories) {
			SocialVertex v = vertexMapping.get(t.getPerson());
			double val_v = getValue(v);
			
			List<Person> alters = new ArrayList<Person>(v.getNeighbours().size());
			for(SocialVertex w : v.getNeighbours())
				alters.add(w.getPerson().getPerson());
			
			for(Person alter : alters) {
				if(tracker.timeOverlap(t.getPerson(), alter) > 0) {
					double val_w = getValue(vertexMapping.get(alter));
					
					xvals.add(val_v);
					yvals.add(val_w);
				}
			}
		}

		double r = new PearsonsCorrelation().correlation(xvals.toNativeArray(), yvals.toNativeArray());
		String key2 = String.format("r_trip_%1$s", key);
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.addValue(r);
		results.put(key2, stats);
		
		try {
			TDoubleDoubleHashMap correl = Correlations.mean(xvals.toNativeArray(), yvals.toNativeArray());
			StatsWriter.writeHistogram(correl, key, key, String.format("%1$s/%2$s.txt", getOutputDirectory(), key2));
			
			TDoubleObjectHashMap<DescriptiveStatistics> table = Correlations.statistics(xvals.toNativeArray(), yvals.toNativeArray(), new DummyDiscretizer());
			StatsWriter.writeBoxplotStats(table, String.format("%1$s/%2$s.table.txt", getOutputDirectory(), key2));
			StatsWriter.writeScatterPlot(table, String.format("%1$s/%2$s.xy.txt", getOutputDirectory(), key2));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected abstract double getValue(SocialVertex v);
}
