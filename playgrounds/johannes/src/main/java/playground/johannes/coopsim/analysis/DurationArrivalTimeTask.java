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
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 * 
 */
public class DurationArrivalTimeTask extends TrajectoryAnalyzerTask {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask#analyze(java
	 * .util.Set, java.util.Map)
	 */
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity) t.getElements().get(i)).getType());
			}
		}

		for (String purpose : purposes) {
			analyzeIntern(trajectories, purpose);
		}
		analyzeIntern(trajectories, null);
	}

	private void analyzeIntern(Set<Trajectory> trajectories, String purpose) {
		TDoubleArrayList arrivals = new TDoubleArrayList(trajectories.size());
		TDoubleArrayList durations = new TDoubleArrayList(trajectories.size());
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				if (purpose == null || ((Activity) t.getElements().get(i)).getType().equals(purpose)) {
					double start = t.getTransitions().get(i);
					double end = t.getTransitions().get(i + 1);
					if ((end - start) > 0) {
						arrivals.add(start);
						durations.add(end - start);
					}
				}
			}
		}

//		TDoubleDoubleHashMap map = Correlations.mean(arrivals.toNativeArray(), durations.toNativeArray(), 3600);
		Discretizer d = FixedSampleSizeDiscretizer.create(arrivals.toNativeArray(), 100, 100);
		TDoubleDoubleHashMap map = Correlations.mean(arrivals.toNativeArray(), durations.toNativeArray(), d);
		try {
			if(purpose == null)
				StatsWriter.writeHistogram(map, "arr", "dur", String.format("%1$s/dur_arr.txt", getOutputDirectory()));
			else
				StatsWriter.writeHistogram(map, "arr", "dur", String.format("%1$s/dur_arr.%2$s.txt", getOutputDirectory(), purpose));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
