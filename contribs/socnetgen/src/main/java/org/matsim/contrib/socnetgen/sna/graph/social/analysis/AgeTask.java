/* *********************************************************************** *
 * project: org.matsim.*
 * AgeTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.social.analysis;

import gnu.trove.map.hash.TDoubleObjectHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialEdge;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class AgeTask extends ModuleAnalyzerTask<Age> {

	public AgeTask() {
		setKey("age");
		setModule(new Age());
	}
	
	public AgeTask(Age module) {
		setKey("age");
		setModule(module);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		DescriptiveStatistics stats = module.statistics(graph.getVertices());
		statsMap.put(key, stats);
		printStats(stats, key);
		
		if(outputDirectoryNotNull()) {
			try {
				writeHistograms(stats, new LinearDiscretizer(1.0), key, false);
			
				StatsWriter.writeHistogram(module.correlation((Set<? extends SocialVertex>) graph.getVertices()), "age", "age_mean", getOutputDirectory() + "/age_age.mean.txt");
			
				TDoubleObjectHashMap<DescriptiveStatistics> stat = module.boxplot((Set<? extends SocialVertex>) graph.getVertices());
				StatsWriter.writeBoxplotStats(stat, getOutputDirectory() + "age_age.table.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		stats = new DescriptiveStatistics();
		stats.addValue(module.correlationCoefficient((Set<? extends SocialEdge>) graph.getEdges()));
		statsMap.put("r_" + key, stats);
		printStats(stats, "r_" + key);
	}

}
