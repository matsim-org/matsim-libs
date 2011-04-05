/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeLengthAccessibilityTask.java
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
import gnu.trove.TObjectDoubleIterator;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 * 
 */
public class EdgeLengthAccessibilityTask extends ModuleAnalyzerTask<Accessibility> {

	private TObjectDoubleHashMap<Vertex> accessValues;

	public EdgeLengthAccessibilityTask(Accessibility module) {
		setModule(module);
	}
	
	public void setAccessValues(TObjectDoubleHashMap<Vertex> accessValues) {
		this.accessValues = accessValues;
	}

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if (accessValues == null)
			accessValues = module.values(graph.getVertices());

//		TObjectDoubleHashMap<Vertex> edgeLengthMean = EdgeLengthMean.getInstance().values(graph.getVertices());

		Discretizer discretizer = FixedSampleSizeDiscretizer.create(accessValues.getValues(), 50, 200);
//		TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(edgeLengthMean, accessValues,
//				discretizer);
		
//		try {
//			TXTWriter.writeMap(correl, "A", "d_mean", getOutputDirectory() + "d_mean_A.txt");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		TObjectDoubleHashMap<Edge> lengths = new EdgeLength().values(graph.getEdges());
		double[] xVals = new double[lengths.size() * 2];
		double[] yVals = new double[lengths.size() * 2];
		TObjectDoubleIterator<Edge> it = lengths.iterator();
		int k = 0;
		for(int i = 0; i < lengths.size(); i++) {
			it.advance();
			Vertex v1 = it.key().getVertices().getFirst();
			Vertex v2 = it.key().getVertices().getSecond();
			
			double a1 = accessValues.get(v1);
			double a2 = accessValues.get(v2);
			
			xVals[k] = a1;
			yVals[k] = it.value();
			
			k++;
			xVals[k] = a2;
			yVals[k] = it.value();
			
			k++;
		}
		
		TDoubleDoubleHashMap correl = Correlations.mean(xVals, yVals, discretizer);
		
		try {
			TXTWriter.writeMap(correl, "A", "d_mean", getOutputDirectory() + "d_mean_A.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
