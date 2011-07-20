/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityDurationTask.java
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
package playground.johannes.socialnetworks.sim.analysis;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * @author illenberger
 *
 */
public class ActivityDurationTask extends TrajectoryAnalyzerTask {

	public final static String KEY = "dur";
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		ActivityDuration duration = new ActivityDuration();
		
		Map<String, DescriptiveStatistics> map = duration.statistics(trajectories);
		for(Entry<String, DescriptiveStatistics> entry : map.entrySet()) {
			String type = entry.getKey();
			DescriptiveStatistics stats = entry.getValue();
			
			String subKey = String.format("%1$s_%2$s", KEY, type);
			results.put(subKey, stats);
			
			if(outputDirectoryNotNull()) {
				try {
					writeHistograms(stats, subKey, 50, 50);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
