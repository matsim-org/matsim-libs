/* *********************************************************************** *
 * project: org.matsim.*
 * TripDistanceTask.java
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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.sim.analysis.Trajectory;
import playground.johannes.socialnetworks.sim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.socialnetworks.sim.gis.ActivityDistanceCalculator;

/**
 * @author illenberger
 *
 */
public class TripDistanceTask extends TrajectoryAnalyzerTask {

	private final ActivityDistanceCalculator calculator;

	public TripDistanceTask(ActivityDistanceCalculator calculator) {
		this.calculator = calculator;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity)t.getElements().get(i)).getType());
			}
		}
		
		for(String purpose : purposes) {
			TripDistanceSum tripDistance = new TripDistanceSum(purpose, calculator);
			DescriptiveStatistics stats = tripDistance.statistics(trajectories);
			
			results.put("d_trip_" + purpose, stats);
			TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 50, 50), true);
			try {
				TXTWriter.writeMap(hist, "d", "p", String.format("%1$s/d_trip_%2$s,txt", getOutputDirectory(), purpose));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
