/* *********************************************************************** *
 * project: org.matsim.*
 * AgeAccessibilityTask.java
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
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.analysis.Accessibility;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 * 
 */
public class AgeAccessibilityTask extends ModuleAnalyzerTask<Accessibility> {

	private TObjectDoubleHashMap<Vertex> accessValues;
	
	public AgeAccessibilityTask(Accessibility module) {
		setModule(module);
	}

	public AgeAccessibilityTask(TObjectDoubleHashMap<Vertex> accessValues) {
		this.accessValues = accessValues;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if (outputDirectoryNotNull()) {
			SpatialGraph spatialGraph = (SpatialGraph) graph;
			
			TObjectDoubleHashMap<Vertex> xVals = new TObjectDoubleHashMap<Vertex>(graph.getVertices().size());
			if(module == null)
				xVals = accessValues;
			else
				xVals = module.values(graph.getVertices());
			
			try {
				TObjectDoubleHashMap<Vertex> yVals = Age.getInstance().values(spatialGraph.getVertices());
				TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(yVals, xVals,
					FixedSampleSizeDiscretizer.create(xVals.getValues(), 50, 100));

				StatsWriter.writeHistogram(correl, "A", "age", getOutputDirectory() + "age_mean_A.txt");
				
				TDoubleObjectHashMap<DescriptiveStatistics> table = VertexPropertyCorrelation.statistics(yVals, xVals, new LinearDiscretizer(xVals.getValues(), 200));
				StatsWriter.writeBoxplotStats(table, String.format("%1$s/age_A.table.txt", getOutputDirectory()));
				StatsWriter.writeScatterPlot(table, String.format("%1$s/age_A.xy.txt", getOutputDirectory()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
