/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeLengthTask.java
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
package org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class EdgeLengthTask extends AnalyzerTask {

	public static final String KEY = "d";

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		EdgeLength edgeLenght = EdgeLength.getInstance();
		
		DescriptiveStatistics stats = edgeLenght.statistics(graph.getEdges());
		printStats(stats, KEY);
		statsMap.put(KEY, stats);
		
		if(outputDirectoryNotNull()) {
			try {
				writeHistograms(stats, new LinearDiscretizer(1000.0), KEY, false);
				writeHistograms(stats, KEY, 50, 1);
				writeCumulativeHistograms(stats, KEY, 100, 100);
				writeRawData(stats, KEY);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
