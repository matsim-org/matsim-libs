/* *********************************************************************** *
 * project: org.matsim.*
 * LegLoadTask.java
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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.sna.util.TXTWriter;

/**
 * @author illenberger
 *
 */
public class LegLoadTask extends TrajectoryAnalyzerTask {

	private final double resolution = 60;
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity)t.getElements().get(i)).getType());
			}
		}
		
		for(String purpose : purposes) {
			TDoubleDoubleHashMap load = legLoad(trajectories, purpose);
			try {
				TXTWriter.writeMap(load, "t", "freq", String.format("%1$s/legload.%2$s.txt", getOutputDirectory(), purpose));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		TDoubleDoubleHashMap load = legLoad(trajectories, null);
		try {
			TXTWriter.writeMap(load, "t", "freq", String.format("%1$s/legload.all.txt", getOutputDirectory()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private TDoubleDoubleHashMap legLoad(Set<Trajectory> trajectories, String type) {
		TDoubleDoubleHashMap loadMap = new TDoubleDoubleHashMap();
		for(Trajectory trajectory : trajectories) {
			for(int i = 1; i < trajectory.getElements().size() - 1; i += 2) {
				Activity act = (Activity) trajectory.getElements().get(i + 1);
				if(type == null || act.getType().equals(type)) {
					int start = (int) (trajectory.getTransitions().get(i)/resolution);
					int end = (int) (trajectory.getTransitions().get(i+1)/resolution);
					for(int time = start; time < end; time++) {
						loadMap.adjustOrPutValue(time, 1, 1);
					}
				}
			}
		}
		
		return loadMap;
	}
}
