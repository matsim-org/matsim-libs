/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeEdgeLength.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class DegreeEdgeLengthTask extends ModuleAnalyzerTask<Degree> {

	private static final Logger logger = Logger.getLogger(DegreeEdgeLengthTask.class);
	
	public DegreeEdgeLengthTask() {
		setModule(new Degree());
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			
		TObjectDoubleHashMap<Vertex> kValues = module.values(graph.getVertices());
		TObjectDoubleHashMap<SpatialVertex> dValues = new Distance().vertexMeanValues((Set<? extends SpatialVertex>) graph.getVertices());
		
		double[] kValues2 = new double[kValues.size()];
		double[] dValues2 = new double[kValues.size()];
		TObjectDoubleIterator<Vertex> it = kValues.iterator();
		for(int i = 0; i < kValues.size(); i++) {
			it.advance();
			kValues2[i] = it.value();
			dValues2[i] = dValues.get((SpatialVertex) it.key());
		}
		
		try {
			Correlations.writeToFile(Correlations.correlationMean(kValues2, dValues2, 5.0), getOutputDirectory() + "/k_distance.txt", "k", "distance_mean");
		} catch (IOException e) {
			e.printStackTrace();
		}
		} else {
			logger.warn("No ouput directory specified!");
		}
	}

}
