/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityStartTimeTask.java
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

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class ArrivalTimeTask extends TrajectoryAnalyzerTask {

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity)t.getElements().get(i)).getType());
			}
		}
		
		for(String purpose : purposes) {
			ArrivalTime startTime = new ArrivalTime(purpose);
			DescriptiveStatistics stats = startTime.statistics(trajectories, true);
			
			String key = "t_arr_" + purpose;
			results.put(key, stats);
			
			try {
				writeHistograms(stats, key, 50, 50);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
