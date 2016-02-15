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

package playground.johannes.coopsim.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Leg;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class TripCountTask extends TrajectoryAnalyzerTask {

	public static final String KEY = "trip.n";
	
	/* (non-Javadoc)
	 * @see playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask#analyze(java.util.Set, java.util.Map)
	 */
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Map<String, PlanElementConditionComposite<Leg>> conditions = Conditions.getLegConditions(trajectories);
		
		TripCount tripCount = new TripCount();
		
		for(Entry<String, PlanElementConditionComposite<Leg>> entry : conditions.entrySet()) {
			tripCount.setCondition(entry.getValue());
			DescriptiveStatistics stats = tripCount.statistics(trajectories);
			
			DescriptiveStatistics sum = new DescriptiveStatistics();
			sum.addValue(stats.getSum());
			
			results.put(String.format("%s.%s", KEY, entry.getKey()), sum);
		}

	}

}
