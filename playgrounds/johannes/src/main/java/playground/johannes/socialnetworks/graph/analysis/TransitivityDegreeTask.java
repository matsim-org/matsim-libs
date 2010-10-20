/* *********************************************************************** *
 * project: org.matsim.*
 * TransitivityDegreeTask.java
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
package playground.johannes.socialnetworks.graph.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.IOException;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.Transitivity;

import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 * 
 */
public class TransitivityDegreeTask extends ModuleAnalyzerTask<Transitivity> {

	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		if (getOutputDirectory() != null) {

			TObjectDoubleHashMap<? extends Vertex> values = module.localClusteringCoefficients(graph.getVertices());

			double[] values1 = new double[values.size()];
			double[] values2 = new double[values.size()];

			TObjectDoubleIterator<? extends Vertex> it = values.iterator();
			for (int i = 0; i < values.size(); i++) {
				it.advance();
				values2[i] = it.value();
				values1[i] = it.key().getNeighbours().size();
			}

			TDoubleDoubleHashMap c = Correlations.correlationMean(values1, values2);
			try {
				Correlations.writeToFile(c, getOutputDirectory() + "/c_k.txt", "k", "c_local");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
