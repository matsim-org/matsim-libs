/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityDuration.java
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;

/**
 * @author illenberger
 *
 */
public class ActivityDuration {
	
	private static final Logger logger = Logger.getLogger(ActivityDuration.class);

	public Map<String, DescriptiveStatistics> statistics(Set<Trajectory> trajectories) {
		Map<String, DescriptiveStatistics> map = new HashMap<String, DescriptiveStatistics>();
		int cnt0 = 0;
		for (Trajectory trajectory : trajectories) {

			for (int i = 0; i < trajectory.getElements().size(); i += 2) {
				
					Activity act = (Activity) trajectory.getElements().get(i);

					String type = act.getType();
					DescriptiveStatistics stats = map.get(type);
					if (stats == null) {
						stats = new DescriptiveStatistics();
						map.put(type, stats);
					}
					
					double start = trajectory.getTransitions().get(i);
//					if(!Double.isNaN(act.getStartTime()) && !Double.isInfinite(act.getStartTime()))
//						start = act.getStartTime();
//					
					double end = trajectory.getTransitions().get(i + 1);
//					if(!Double.isNaN(act.getEndTime()) && !Double.isInfinite(act.getEndTime()))
//						end = act.getEndTime();

//					start = Math.min(start, end);
					
					double duration = end - start;
					
					if(duration > 0)
						stats.addValue(duration);
					else
						cnt0++;
				
			}
		}
		
		if(cnt0 > 0)
			logger.warn(String.format("Ignored %1$s activities with duration zero.", cnt0));
		
		return map;
	}
}
