/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeNormConstantTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.SpatialCostFunction;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;

/**
 * @author illenberger
 *
 */
public class DegreeAccessibilityTask extends ModuleAnalyzerTask<Accessibility> {

	public DegreeAccessibilityTask(Accessibility module) {
		setModule(module);
	}
	
	public DegreeAccessibilityTask(SpatialCostFunction function) {
		setModule(new Accessibility(function));
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> statsMap) {
		if(outputDirectoryNotNull()) {
			SpatialGraph graph = (SpatialGraph) g;
			
			TObjectDoubleHashMap<Vertex> xVals = module.values(graph.getVertices());
			TObjectDoubleHashMap<Vertex> yVals = Degree.getInstance().values(graph.getVertices());
			TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(yVals, xVals, FixedSampleSizeDiscretizer.create(xVals.getValues(), 50, 100));
			try {
				TXTWriter.writeMap(correl, "A", "k", getOutputDirectory() + "k_mean_A.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
