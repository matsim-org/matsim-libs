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
import org.matsim.contrib.common.stats.DummyDiscretizer;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class LegFrequencyTask extends TrajectoryAnalyzerTask {
	
	public static final String KEY = "leg.n";

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> modes = TrajectoryUtils.getModes(trajectories);
		modes.add(null);
		
		for (String mode : modes) {
			PlanElementCondition<Leg> condition;
			if (mode != null) {
				condition = new LegModeCondition(mode);
			} else {
				condition = DefaultCondition.getInstance();
			}

			DescriptiveStatistics stats = new DescriptiveStatistics();
			for (Trajectory t : trajectories) {
				int cnt = 0;
				for (int i = 1; i < t.getElements().size(); i += 2) {
					if(condition.test(t, (Leg) t.getElements().get(i), i)) {
						cnt++;
					}
				}
				stats.addValue(cnt);
			}
			
			String key;
			if(mode == null)
				 key = String.format("%s.all", KEY, mode);
			else
				 key = String.format("%s.%s", KEY, mode);
			
			results.put(key, stats);
			
			if(outputDirectoryNotNull()) {
				try {
					writeHistograms(stats, DummyDiscretizer.getInstance(), key, false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

}
