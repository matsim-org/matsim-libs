/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeLengthDegreeTask.java
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
import gnu.trove.TObjectDoubleIterator;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Degree;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.socialnetworks.statistics.Correlations;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class EdgeLengthDegreeTask extends ModuleAnalyzerTask<Degree> {

	public EdgeLengthDegreeTask() {
		setModule(Degree.getInstance());
	}
	
	public EdgeLengthDegreeTask(Degree module) {
		setModule(module);
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<Vertex> degreeValues = module.values(graph.getVertices());
		Discretizer discretizer = FixedSampleSizeDiscretizer.create(degreeValues.getValues(), 1, 50);
		
		TObjectDoubleHashMap<Edge> lengths = new EdgeLength().values(graph.getEdges());
		double[] xVals = new double[lengths.size() * 2];
		double[] yVals = new double[lengths.size() * 2];
		TObjectDoubleIterator<Edge> it = lengths.iterator();
		int k = 0;
		for(int i = 0; i < lengths.size(); i++) {
			it.advance();
			Vertex v1 = it.key().getVertices().getFirst();
			Vertex v2 = it.key().getVertices().getSecond();
			
			double k1 = degreeValues.get(v1);
			double k2 = degreeValues.get(v2);
			
			xVals[k] = k1;
			yVals[k] = it.value();
			
			k++;
			xVals[k] = k2;
			yVals[k] = it.value();
			
			k++;
		}
		
		TDoubleDoubleHashMap correl = Correlations.mean(xVals, yVals, discretizer);
		
		try {
			StatsWriter.writeHistogram(correl, "k", "d_mean", getOutputDirectory() + "d_mean_k.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
