/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialPropertyAccessibilityTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.Transitivity;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.DummyDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.SpatialCostFunction;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class SpatialPropertyAccessibilityTask extends ModuleAnalyzerTask<Accessibility> {

	private Discretizer discretizer = new DummyDiscretizer();

	private SpatialCostFunction costFunction;

	private Set<Point> points;

	public SpatialPropertyAccessibilityTask(SpatialCostFunction costFunction, Set<Point> points) {
		this.costFunction = costFunction;
		this.points = points;
	}
	
	public void setDiscretizer(Discretizer discretizer) {
		this.discretizer = discretizer;
	}

	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		if (outputDirectoryNotNull()) {
			try {
				SpatialGraph graph = (SpatialGraph) g;
				TObjectDoubleHashMap<SpatialVertex> xVals = module.values(graph.getVertices(), costFunction, points);
				/*
				 * degree-accessibility correlation
				 */
				TObjectDoubleHashMap<Vertex> yVals = Degree.getInstance().values(graph.getVertices());
				TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(yVals, xVals, discretizer);
				TXTWriter.writeMap(correl, "A", "k", getOutputDirectory() + "k_A.mean.txt");
				
				TDoubleObjectHashMap<DescriptiveStatistics> statistics = VertexPropertyCorrelation.statistics(yVals, xVals, discretizer);
				TXTWriter.writeStatistics(statistics, "A", getOutputDirectory() + "k_A.stats.txt");
				/*
				 * transitivity-accessibility correlation
				 */
				yVals = Transitivity.getInstance().values(graph.getVertices());
				correl = VertexPropertyCorrelation.mean(yVals, xVals, discretizer);
				TXTWriter.writeMap(correl, "A", "c_local", getOutputDirectory() + "c_local_A.mean.txt");
				
				statistics = VertexPropertyCorrelation.statistics(yVals, xVals, discretizer);
				TXTWriter.writeStatistics(statistics, "A", getOutputDirectory() + "c_local_A.stats.txt");
				/*
				 * mean distance-accessibility correlation
				 */
				TObjectDoubleHashMap<SpatialVertex> dVals = Distance.getInstance().vertexMean(graph.getVertices());
				correl = VertexPropertyCorrelation.mean(dVals, xVals, discretizer);
				TXTWriter.writeMap(correl, "A", "d_mean", getOutputDirectory() + "d_mean_A.mean.txt");
				
				statistics = VertexPropertyCorrelation.statistics(dVals, xVals, discretizer);
				TXTWriter.writeStatistics(statistics, "A", getOutputDirectory() + "d_mean_A.stats.txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
