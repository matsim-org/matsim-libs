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
import org.apache.log4j.Logger;

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 * 
 */
public abstract class AbstractTrajectoryProperty implements TrajectoryProperty {

	private static final Logger logger = Logger.getLogger(AbstractTrajectoryProperty.class);
	
	public DescriptiveStatistics statistics(Set<? extends Trajectory> trajectories, boolean ignoreZeros) {
		if (!ignoreZeros)
			return this.statistics(trajectories);
		else {
			TObjectDoubleHashMap<Trajectory> values = values(trajectories);
			DescriptiveStatistics stats = new DescriptiveStatistics();

			int zeros = 0;
			TObjectDoubleIterator<Trajectory> it = values.iterator();
			for (int i = 0; i < values.size(); i++) {
				it.advance();
				if(it.value() > 0)
					stats.addValue(it.value());
				else
					zeros++;
			}

			if(zeros > 0)
				logger.debug(String.format("Ignored %1$s zero values.", zeros));
			
			return stats;
		}
	}

	@Override
	public DescriptiveStatistics statistics(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = values(trajectories);
		DescriptiveStatistics stats = new DescriptiveStatistics();

		TObjectDoubleIterator<Trajectory> it = values.iterator();
		for (int i = 0; i < values.size(); i++) {
			it.advance();
			stats.addValue(it.value());
		}

		return stats;
	}

}
