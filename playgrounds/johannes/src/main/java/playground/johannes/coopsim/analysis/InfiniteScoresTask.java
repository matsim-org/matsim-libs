/* *********************************************************************** *
 * project: org.matsim.*
 * InfinitScoresTask.java
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

import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class InfiniteScoresTask extends TrajectoryAnalyzerTask {

	private static final Logger logger = Logger.getLogger(InfiniteScoresTask.class);
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		int count = 0;
		for(Trajectory t : trajectories) {
			Plan p = t.getPerson().getSelectedPlan();
			if(Double.isInfinite(p.getScore()))
				count++;
		}
		
		if(count > 0)
			logger.warn(String.format("There are %1$s plans with infinite score.", count));

	}

}
