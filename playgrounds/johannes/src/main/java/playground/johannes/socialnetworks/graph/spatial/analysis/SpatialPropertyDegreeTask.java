/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialPropertyDegreeTask.java
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
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
public class SpatialPropertyDegreeTask extends ModuleAnalyzerTask<Degree> {

	private Discretizer discretizer = new DummyDiscretizer();
	
	private SpatialCostFunction costFunction;
	
	private Set<Point> opportunities;
	
	public SpatialPropertyDegreeTask(SpatialCostFunction costFunction, Set<Point> points) {
		this.costFunction = costFunction;
		this.opportunities = points;
		setModule(Degree.getInstance());
	}
	
	public void setDiscretizer(Discretizer discretizer) {
		this.discretizer = discretizer;
	}
	
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		if(outputDirectoryNotNull()) {
			try {
				SpatialGraph graph = (SpatialGraph) g;
				
				if(opportunities == null) {
					opportunities = new HashSet<Point>();
					for(SpatialVertex vertex : graph.getVertices())
						opportunities.add(vertex.getPoint());
				}
				
				/*
				 * distance-degree correlation
				 */
				TObjectDoubleHashMap<SpatialVertex> yVals = Distance.getInstance().vertexMean(graph.getVertices());
				TObjectDoubleHashMap<Vertex> kVals =  module.values(graph.getVertices());
				TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(yVals, kVals, discretizer);
				TXTWriter.writeMap(correl, "k", "d_mean", getOutputDirectory() + "d_mean_k.mean.txt");
				
				TDoubleObjectHashMap<DescriptiveStatistics> stat = VertexPropertyCorrelation.statistics(yVals, kVals, discretizer);
				TXTWriter.writeBoxplotStats(stat, getOutputDirectory() + "d_mean_k.boxplot.txt");
				TXTWriter.writeStatistics(stat, "k", getOutputDirectory() + "d_k.stats.txt");
				
				yVals = Distance.getInstance().vertexMedian(graph.getVertices());
				correl = VertexPropertyCorrelation.mean(yVals, kVals, discretizer);
				TXTWriter.writeMap(correl, "k", "d_median", getOutputDirectory() + "d_median_k.mean.txt");
				
				stat = VertexPropertyCorrelation.statistics(yVals, kVals, discretizer);
				TXTWriter.writeBoxplotStats(stat, getOutputDirectory() + "d_median_k.boxplot.txt");
				/*
				 * accessibility-degree correlation
				 */
//				yVals = Accessibility.getInstance().values(graph.getVertices(), costFunction, opportunities);
//				correl = VertexPropertyCorrelation.mean(yVals, kVals, discretizer);
//				TXTWriter.writeMap(correl, "k", "A", getOutputDirectory() + "A_k.mean.txt");
//				stat = VertexPropertyCorrelation.statistics(yVals, kVals, discretizer);
//				TXTWriter.writeStatistics(stat, "k", getOutputDirectory() + "A_k.stats.txt");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
