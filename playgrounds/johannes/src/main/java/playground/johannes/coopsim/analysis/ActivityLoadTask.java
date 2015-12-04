/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLoadTask.java
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

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
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
public class ActivityLoadTask extends TrajectoryAnalyzerTask {

	private final double resolution = 60;

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity)t.getElements().get(i)).getType());
			}
		}
		
		for(String purpose : purposes) {
			TDoubleDoubleHashMap load = activityLoad(trajectories, purpose);
			try {
				StatsWriter.writeHistogram(load, "t", "freq", String.format("%1$s/actload.%2$s.txt", getOutputDirectory(), purpose));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		TDoubleDoubleHashMap load = activityLoad(trajectories, null);
		try {
			StatsWriter.writeHistogram(load, "t", "freq", String.format("%1$s/actload.all.txt", getOutputDirectory()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private TDoubleDoubleHashMap activityLoad(Set<Trajectory> trajectories, String type) {
		TDoubleDoubleHashMap loadMap = new TDoubleDoubleHashMap();
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				Activity act = (Activity) t.getElements().get(i);
				if(type == null || act.getType().equals(type)) {
					int start = (int) (t.getTransitions().get(i)/resolution);
					int end = (int) (t.getTransitions().get(i+1)/resolution);
					for(int time = start; time < end; time++) {
						loadMap.adjustOrPutValue(time, 1, 1);
					}
				}
			}
		}
		
		return loadMap;
	}
}
