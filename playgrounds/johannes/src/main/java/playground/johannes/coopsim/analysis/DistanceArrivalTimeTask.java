/* *********************************************************************** *
 * project: org.matsim.*
 * DurrationArrivalTimeTask.java
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
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 * 
 */
public class DistanceArrivalTimeTask extends TrajectoryAnalyzerTask {

	private TripDistanceMean tripDistanceMean;
	
	public DistanceArrivalTimeTask(TripDistanceMean distances) {
		this.tripDistanceMean = distances;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<Trajectory> distances = tripDistanceMean.values(trajectories);
		
		Set<String> purposes = new HashSet<String>();
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity) t.getElements().get(i)).getType());
			}
		}

		for (String purpose : purposes) {
			analyzeIntern(trajectories, purpose, distances);
		}
		analyzeIntern(trajectories, null, distances);
	}

	private void analyzeIntern(Set<Trajectory> trajectories, String purpose, TObjectDoubleHashMap<Trajectory> distancesMap) {
		TDoubleArrayList arrivals = new TDoubleArrayList(trajectories.size());
		TDoubleArrayList distances = new TDoubleArrayList(trajectories.size());
		
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				if (purpose == null || ((Activity) t.getElements().get(i)).getType().equals(purpose)) {
					double start = t.getTransitions().get(i);
					arrivals.add(start);
					distances.add(distancesMap.get(t));
				}
			}
		}

		TDoubleDoubleHashMap map = Correlations.mean(arrivals.toNativeArray(), distances.toNativeArray(), 3600);
//		Discretizer d = FixedSampleSizeDiscretizer.create(arrivals.toNativeArray(), 100, 100);
//		TDoubleDoubleHashMap map = Correlations.mean(arrivals.toNativeArray(), distances.toNativeArray(), d);
		try {
			if(purpose == null)
				StatsWriter.writeHistogram(map, "arr", "dist", String.format("%1$s/dist_arr.txt", getOutputDirectory()));
			else
				StatsWriter.writeHistogram(map, "arr", "dist", String.format("%1$s/dist_arr.%2$s.txt", getOutputDirectory(), purpose));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
