/* *********************************************************************** *
 * project: org.matsim.*
 * TransitivityAccessibilityTask.java
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Transitivity;
import org.matsim.contrib.socnetgen.sna.graph.analysis.VertexPropertyCorrelation;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class TransitivityAccessibilityTask extends ModuleAnalyzerTask<Accessibility> {

	private TObjectDoubleHashMap<Vertex> accessValues;
	
	public TransitivityAccessibilityTask(Accessibility module) {
		setModule(module);
	}
	
	public void setAccessValues(TObjectDoubleHashMap<Vertex> accessValues) {
		this.accessValues = accessValues;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if(accessValues == null) {
			accessValues = module.values(graph.getVertices());
		}
		
		TObjectDoubleHashMap<Vertex> localClustering = Transitivity.getInstance().values(graph.getVertices());
		
		Discretizer discretizer = FixedSampleSizeDiscretizer.create(accessValues.getValues(), 1, 30);
		TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(localClustering, accessValues,	discretizer);
		
		try {
			StatsWriter.writeHistogram(correl, "A", "c_local_mean", getOutputDirectory() + "c_local_mean_A.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
