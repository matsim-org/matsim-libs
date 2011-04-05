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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;

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
			
			TObjectDoubleHashMap<Vertex> yVals = Age.getInstance().values(spatialGraph.getVertices());
			TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(yVals, xVals,
					FixedSampleSizeDiscretizer.create(xVals.getValues(), 50, 50));

			try {
				TXTWriter.writeMap(correl, "A", "age", getOutputDirectory() + "age_mean_A.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
