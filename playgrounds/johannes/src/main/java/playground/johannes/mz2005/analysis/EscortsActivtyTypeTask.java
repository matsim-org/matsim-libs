/* *********************************************************************** *
 * project: org.matsim.*
 * EscortsActivtyTypeTask.java
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
package playground.johannes.mz2005.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.mz2005.io.EscortData;

/**
 * @author illenberger
 * 
 */
public class EscortsActivtyTypeTask extends TrajectoryAnalyzerTask {

	private final EscortData escortData;

	public EscortsActivtyTypeTask(EscortData data) {
		this.escortData = data;
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Map<String, DescriptiveStatistics> statsMap = new HashMap<String, DescriptiveStatistics>();
		for (Trajectory trajectory : trajectories) {
			for (int i = 2; i < trajectory.getElements().size(); i += 2) {
				Activity destination = (Activity) trajectory.getElements().get(i);
				int escorts = escortData.getEscorts(trajectory.getPerson(), i - 1);

				if (escorts > 0) {
					DescriptiveStatistics stats = statsMap.get(destination.getType());
					if (stats == null) {
						stats = new DescriptiveStatistics();
						statsMap.put(destination.getType(), stats);
					}

					stats.addValue(escorts);
				}
			}
		}

		try {
			TXTWriter.writeStatistics(statsMap, getOutputDirectory() + "/escorts_type.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
