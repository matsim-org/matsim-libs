/* *********************************************************************** *
 * project: org.matsim.*
 * CoordinationComplexityTask.java
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
import org.matsim.contrib.common.stats.TXTWriter;
import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 * 
 */
public class CoordinationComplexityTask extends TrajectoryAnalyzerTask {

	private final VisitorTracker tracker;

	private final SocialGraph graph;

	private final Map<Person, ActivityDesires> desires;

	public CoordinationComplexityTask(VisitorTracker tracker, Map<Person, ActivityDesires> desires, SocialGraph graph) {
		this.tracker = tracker;
		this.desires = desires;
		this.graph = graph;
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<Trajectory> members = new ActivityGroupMembers(tracker, graph).values(trajectories);
		TObjectDoubleHashMap<Trajectory> arrivals = new DesiredArrivalTimeDiff(desires).values(trajectories);
		TObjectDoubleHashMap<Trajectory> durations = new DesiredDurationDiff(desires).values(trajectories);
		TObjectDoubleHashMap<Trajectory> types = new DesiredActivityTypeDiff(desires).values(trajectories);

		Set<String> purposes = new HashSet<String>();
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity) t.getElements().get(i)).getType());
			}
		}
		purposes.add(null);

		for (String purpose : purposes) {
			TDoubleArrayList xvals = new TDoubleArrayList(trajectories.size());
			TDoubleArrayList arrVals = new TDoubleArrayList(trajectories.size());
			TDoubleArrayList durVals = new TDoubleArrayList(trajectories.size());
			TDoubleArrayList typeVals = new TDoubleArrayList(trajectories.size());
			
			for (Trajectory t : trajectories) {
				Activity act = (Activity) t.getElements().get(2);
				if (purpose == null || act.getType().equals(purpose)) {
					xvals.add(members.get(t));
					arrVals.add(Math.abs(arrivals.get(t)));
					durVals.add(Math.abs(durations.get(t)));
					typeVals.add(types.get(t));
				}
			}
			
			try {
				double[] x = xvals.toNativeArray();
				
				if(purpose == null)
					purpose = "all";
					
				TDoubleDoubleHashMap correl = Correlations.mean(x, arrVals.toNativeArray());
				TXTWriter.writeMap(correl, "members", "diff", String.format("%1$s/arrdiff_members.%2$s.txt", getOutputDirectory(), purpose));
				
				correl = Correlations.mean(x, durVals.toNativeArray());
				TXTWriter.writeMap(correl, "members", "diff", String.format("%1$s/durdiff_members.%2$s.txt", getOutputDirectory(), purpose));
				
				correl = Correlations.mean(x, typeVals.toNativeArray());
				TXTWriter.writeMap(correl, "members", "diff", String.format("%1$s/typediff_members.%2$s.txt", getOutputDirectory(), purpose));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
