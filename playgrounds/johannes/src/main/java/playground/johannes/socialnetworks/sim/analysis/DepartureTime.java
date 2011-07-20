/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureTime.java
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
import org.matsim.api.core.v01.population.Activity;

/**
 * @author illenberger
 *
 */
public class DepartureTime {

	public Map<String, DescriptiveStatistics> statistics(Set<Trajectory> trajectories) {
		Map<String, DescriptiveStatistics> map = new HashMap<String, DescriptiveStatistics>();
		
		for(Trajectory t : trajectories) {
			for(int i = 1; i < t.getElements().size(); i += 2) {
				double depTime = t.getTransitions().get(i);
				String type = ((Activity)t.getElements().get(i + 1)).getType();
				
				DescriptiveStatistics stats = map.get(type);
				if(stats == null) {
					stats = new DescriptiveStatistics();
					map.put(type, stats);
				}
				
				stats.addValue(depTime);
			}
		}
		
		return map;
	}
}
