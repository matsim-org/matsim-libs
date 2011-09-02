/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractTrajectoryProperty.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.socialnetworks.sim.analysis.Trajectory;

/**
 * @author illenberger
 *
 */
public abstract class AbstractTrajectoryProperty implements TrajectoryProperty {

	@Override
	public DescriptiveStatistics statistics(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = values(trajectories);
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		TObjectDoubleIterator<Trajectory> it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			stats.addValue(it.value());
		}
		
		return stats;
	}

}
