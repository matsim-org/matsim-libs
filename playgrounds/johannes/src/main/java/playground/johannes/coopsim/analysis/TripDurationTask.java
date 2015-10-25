/* *********************************************************************** *
 * project: org.matsim.*
 * TripDurationTask.java
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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Leg;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class TripDurationTask extends TrajectoryAnalyzerTask {

	/* (non-Javadoc)
	 * @see playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask#analyze(java.util.Set, java.util.Map)
	 */
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Map<String, PlanElementConditionComposite<Leg>> conditions = Conditions.getLegConditions(trajectories);
		
		TripDuration duration = new TripDuration();
		for(Entry<String, PlanElementConditionComposite<Leg>> entry : conditions.entrySet()) {
			duration.setCondition(entry.getValue());
			DescriptiveStatistics stats = duration.statistics(trajectories, true);
			
			String key = "t.trip." + entry.getKey();
			results.put(key, stats);
			
			try {
//				writeHistograms(stats, new LinearDiscretizer(60.0), key, false);
				writeHistograms(stats, key, 100, 50);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
