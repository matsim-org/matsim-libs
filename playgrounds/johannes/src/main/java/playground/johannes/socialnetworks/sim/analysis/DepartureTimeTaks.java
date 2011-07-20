/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureTimeTaks.java
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
public class DepartureTimeTaks extends TrajectoryAnalyzerTask {

	private final String KEY = "t_dep";
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DepartureTime depTime = new DepartureTime();
		
		Map<String, DescriptiveStatistics> map = depTime.statistics(trajectories);
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
