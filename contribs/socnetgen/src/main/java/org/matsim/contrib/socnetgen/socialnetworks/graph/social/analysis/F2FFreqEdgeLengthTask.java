/* *********************************************************************** *
 * project: org.matsim.*
 * F2FFreqEdgeLengthTask.java
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
package org.matsim.contrib.socnetgen.socialnetworks.graph.social.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.analysis.EdgeLength;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class F2FFreqEdgeLengthTask extends AnalyzerTask {

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		EdgeLength len = new EdgeLength();
		len.setIgnoreZero(true);
		
		TObjectDoubleHashMap<Edge> lenVals = len.values(graph.getEdges());
		Discretizer discretizer = FixedSampleSizeDiscretizer.create(lenVals.getValues(), 200, 100);
		
		
		TDoubleDoubleHashMap hist = VertexPropertyCorrelation.mean(F2FFrequency.getInstance(), len, graph.getEdges(), discretizer);
		try {
			StatsWriter.writeHistogram(hist, "d", "f", getOutputDirectory() + "/f2ffreq_d.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
