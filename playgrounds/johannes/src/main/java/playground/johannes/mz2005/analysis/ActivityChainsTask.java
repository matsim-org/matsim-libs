/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityChainsTask.java
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

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.sna.math.DummyDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class ActivityChainsTask extends TrajectoryAnalyzerTask {

	private static final String KEY = "n_act";
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		TObjectDoubleHashMap<String> chains = new TObjectDoubleHashMap<String>();
		
		for(Trajectory trajectory : trajectories) {
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < trajectory.getElements().size(); i += 2) {
				String type = ((Activity) trajectory.getElements().get(i)).getType();
				builder.append(type);
				builder.append("-");
			}
			
			String chain = builder.toString();
			chains.adjustOrPutValue(chain, 1, 1);
			
			stats.addValue((trajectory.getElements().size() + 1)/2);
		}
		
		results.put(KEY, stats);
		
		if(outputDirectoryNotNull()) {
			try {
				TXTWriter.writeMap(chains, "chain", "n", getOutputDirectory() + "/actchains.txt", true);
				
				writeHistograms(stats, new DummyDiscretizer(), KEY, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
