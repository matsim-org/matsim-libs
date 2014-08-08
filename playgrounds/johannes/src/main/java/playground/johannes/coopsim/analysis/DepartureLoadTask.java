/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureLoadTask.java
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.util.TXTWriter;

/**
 * @author illenberger
 *
 */
public class DepartureLoadTask extends TrajectoryAnalyzerTask {

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity)t.getElements().get(i)).getType());
			}
		}
		
		purposes.add(null);
		
		for(String purpose : purposes) {
			analyze(trajectories, purpose);
		}
	}

	private void analyze(Set<Trajectory> trajectories, String type) {
		TDoubleArrayList samples = new TDoubleArrayList(trajectories.size());
		
		for(Trajectory t : trajectories) {
			for(int i = 1; i < t.getTransitions().size() - 1; i += 2) {
				Activity act = (Activity) t.getElements().get(i + 1);
				if(type == null || act.getType().equals(type)) {
					double time = t.getTransitions().get(i);
					samples.add(time);
				}
			}
		}
		
		try {
			if(type == null)
				type = "all";
			
			if(!samples.isEmpty()) {
				TDoubleDoubleHashMap load = Histogram.createHistogram(samples.toNativeArray(), FixedSampleSizeDiscretizer.create(samples.toNativeArray(), 50, 50), true);
				TXTWriter.writeMap(load, "time", "n", String.format("%1$s/depload.%2$s.txt", getOutputDirectory(), type));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
