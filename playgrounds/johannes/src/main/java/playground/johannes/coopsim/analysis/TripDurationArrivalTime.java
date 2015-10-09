/* *********************************************************************** *
 * project: org.matsim.*
 * TripDurationArrivalTime.java
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
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.TXTWriter;
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
public class TripDurationArrivalTime extends TrajectoryAnalyzerTask {

	/* (non-Javadoc)
	 * @see playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask#analyze(java.util.Set, java.util.Map)
	 */
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for (Trajectory t : trajectories) {
			for (int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity) t.getElements().get(i)).getType());
			}
		}

		purposes.add(null);
		for (String purpose : purposes) {
			analyze(trajectories, purpose);
		}

	}

	public void analyze(Set<Trajectory> trajectories, String type) {
		TDoubleArrayList arrivals = new TDoubleArrayList(trajectories.size());
		TDoubleArrayList durations = new TDoubleArrayList(trajectories.size());
		
		for(Trajectory t : trajectories) {
			for(int i = 2; i < t.getElements().size(); i += 2) {
				Activity act = (Activity) t.getElements().get(i);
				if(type == null || act.getType().endsWith(type)) {
					double start = t.getTransitions().get(i - 1);
					double end = t.getTransitions().get(i);
					
					arrivals.add(end);
					durations.add(end- start);
				}
			}
		}
		
		Discretizer disc = FixedSampleSizeDiscretizer.create(arrivals.toNativeArray(), 50, 50);
		TDoubleDoubleHashMap map = Correlations.mean(arrivals.toNativeArray(), durations.toNativeArray(), disc);
		if(type == null)
			type = "all";
		
		try {
			TXTWriter.writeMap(map, "arrival", "tripDuration", String.format("%1$s/tripdur_arr.%2$s.txt", getOutputDirectory(), type));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
