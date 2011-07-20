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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.util.TXTWriter;

/**
 * @author illenberger
 *
 */
public class LegLoadTask extends TrajectoryAnalyzerTask {

	private final static String KEY = "enroute";
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		LegLoad load = new LegLoad();
		Map<String, TDoubleDoubleHashMap> loadCurves = load.statistics(trajectories);
		
		for(Entry<String, TDoubleDoubleHashMap> entry : loadCurves.entrySet()) {
			String type = entry.getKey();
			TDoubleDoubleHashMap curve = entry.getValue();
			
			DescriptiveStatistics stats = new DescriptiveStatistics();
			for(double val : curve.getValues())
				stats.addValue(val);
			
			String subKey = String.format("%1$s_%2$s", KEY, type);
			
			results.put(subKey, stats);
			
			if(outputDirectoryNotNull()) {
				try {
					TXTWriter.writeMap(curve, "t", "n", String.format("%1$s/%2$s.txt", getOutputDirectory(), subKey));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
