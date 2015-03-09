/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.facilities.ActivityFacilities;

import playground.johannes.coopsim.analysis.DefaultCondition;
import playground.johannes.coopsim.analysis.LegModeCondition;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TripDistanceMean;
import playground.johannes.coopsim.analysis.TripDuration;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author johannes
 *
 */
public class SpeedFactorTask extends TrajectoryAnalyzerTask {

	public static final String KEY = "speedFactor";
	
	private final ActivityFacilities facilities;
	
	public SpeedFactorTask(ActivityFacilities facilities) {
		this.facilities = facilities;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> modes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 1; i < t.getElements().size(); i += 2) {
				modes.add(((Leg)t.getElements().get(i)).getMode());
			}
		}
		
		modes.add(null);
		
		TripDistanceMean dist = new TripDistanceMean(facilities);
		TripDuration durs = new TripDuration();
		for(String mode : modes) {
			if(mode == null) {
				dist.setCondition(DefaultCondition.getInstance());
				durs.setCondition(DefaultCondition.getInstance());
			} else {
				LegModeCondition condition = new LegModeCondition(mode);
				dist.setCondition(condition);
				durs.setCondition(condition);
			}
			TObjectDoubleHashMap<Trajectory> distances = dist.values(trajectories);
			TObjectDoubleHashMap<Trajectory> durations = durs.values(trajectories);
			
			TDoubleArrayList distArray = new TDoubleArrayList(distances.size());
			TDoubleArrayList durArray = new TDoubleArrayList(durations.size());
			
			double distSum = 0;
			double durSum = 0;
			
			TObjectDoubleIterator<Trajectory> it = distances.iterator();
			for(int i = 0; i < distances.size(); i++) {
				it.advance();
				
				distArray.add(it.value());
				distSum += it.value();
				
				double dur = durations.get(it.key());
				durArray.add(dur);
				durSum += dur;
			}
			
			if(mode == null)
				mode = "all";
		
			String key = String.format("%s.%s", KEY, mode);
			
			double factor = distSum/durSum;
			
			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(factor);
			results.put(key, stats);
			
			if(outputDirectoryNotNull()) {
		
			TDoubleDoubleHashMap map = Correlations.mean(distArray.toNativeArray(), durArray.toNativeArray(), new LinearDiscretizer(1000));
			try {
				TXTWriter.writeMap(map, "Distance", "Traveltime", getOutputDirectory() + "/" + key + ".txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			}	
		}

	}

}
