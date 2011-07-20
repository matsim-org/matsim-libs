/* *********************************************************************** *
 * project: org.matsim.*
 * LegDuration.java
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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 *
 */
public class LegDuration {

	private static final Logger logger = Logger.getLogger(LegDuration.class);
	
	public Map<String, DescriptiveStatistics> statisticsPerType(Set<Trajectory> trajectories) {
		Map<String, DescriptiveStatistics> map = new HashMap<String, DescriptiveStatistics>();
		int cnt0 = 0;
		for (Trajectory trajectory : trajectories) {

			for (int i = 1; i < trajectory.getElements().size(); i += 2) {
				if (trajectory.getElements().size() > i + 1) {
					Activity next = (Activity) trajectory.getElements().get(i + 1);
//					Activity prev = (Activity) plan.getElements().get(i - 1);

					String type = next.getType();
					DescriptiveStatistics stats = map.get(type);
					if (stats == null) {
						stats = new DescriptiveStatistics();
						map.put(type, stats);
					}
					
					double start = trajectory.getTransitions().get(i);
//					if(!Double.isNaN(next.getStartTime()) && !Double.isInfinite(next.getStartTime()))
//						start = next.getStartTime();
					
					double end = trajectory.getTransitions().get(i + 1);
//					if(!Double.isNaN(prev.getEndTime()) && !Double.isInfinite(prev.getEndTime()))
//						end = prev.getEndTime();

//					start = Math.min(start, end);
					
					double duration = end - start;
					
//					double duration = next.getStartTime() - prev.getEndTime();
					
					if(duration > 0)
						stats.addValue(duration);
					else
						cnt0++;
				}
			}
		}
		
		if(cnt0 > 0)
			logger.warn(String.format("Ignored %1$s legs with duration zero.", cnt0));
		
		return map;
	}
	
	public Map<String, DescriptiveStatistics> statisticsPerModeAndType(Set<Trajectory> trajectories) {
		Map<String, DescriptiveStatistics> map = new HashMap<String, DescriptiveStatistics>();
		int cnt0 = 0;
		for (Trajectory trajectory : trajectories) {

			for (int i = 1; i < trajectory.getElements().size(); i += 2) {
				if (trajectory.getElements().size() > i + 1) {
					Activity next = (Activity) trajectory.getElements().get(i + 1);
					Leg leg = (Leg) trajectory.getElements().get(i);
					Activity prev = (Activity) trajectory.getElements().get(i - 1);

					String mode = leg.getMode();
					String type = next.getType();
					StringBuilder builder = new StringBuilder(mode.length() + type.length() + 1);
					builder.append(type);
					builder.append("_");
					builder.append(mode);
					String key = builder.toString();
					
					DescriptiveStatistics stats = map.get(key);
					if (stats == null) {
						stats = new DescriptiveStatistics();
						map.put(key, stats);
					}
					
					double start = trajectory.getTransitions().get(i);
//					if(!Double.isNaN(next.getStartTime()) && !Double.isInfinite(next.getStartTime()))
//						start = next.getStartTime();
					
					double end = trajectory.getTransitions().get(i + 1);;
//					if(!Double.isNaN(prev.getEndTime()) && !Double.isInfinite(prev.getEndTime()))
//						end = prev.getEndTime();

					double duration = end - start;
					
					if(duration > 0)
						stats.addValue(duration);
					else
						cnt0++;
				}
			}
		}
		
		if(cnt0 > 0)
			logger.warn(String.format("Ignored %1$s legs with duration zero.", cnt0));
		
		return map;

	}
}
