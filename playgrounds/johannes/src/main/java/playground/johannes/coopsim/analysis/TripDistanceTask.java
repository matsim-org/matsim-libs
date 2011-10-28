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

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;


/**
 * @author illenberger
 *
 */
public class TripDistanceTask extends TrajectoryAnalyzerTask {

	private final ActivityFacilities facilities;
	
	private final DistanceCalculator calculator;
	
	public TripDistanceTask(ActivityFacilities facilities) {
		this.facilities = facilities;
		calculator = OrthodromicDistanceCalculator.getInstance();
	}
	
	public TripDistanceTask(ActivityFacilities facilities, DistanceCalculator calculator) {
		this.facilities = facilities;
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
			TripDistanceSum tripDistance = new TripDistanceSum(purpose, facilities, calculator);
			DescriptiveStatistics stats = tripDistance.statistics(trajectories, true);
			
			String key = "d_trip_" + purpose;
			results.put(key, stats);
			try {
				writeHistograms(stats, key, 50, 50);
				
				double[] values = stats.getValues();
				LinearDiscretizer lin = new LinearDiscretizer(500.0);
				DescriptiveStatistics linStats = new DescriptiveStatistics();
				for(double d : values)
					linStats.addValue(lin.discretize(d));
				
				TrajectoryAnalyzerTask.overwriteStratification(50, 1);
				writeHistograms(linStats, key + ".lin", 50, 50);
				TrajectoryAnalyzerTask.overwriteStratification(30, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
