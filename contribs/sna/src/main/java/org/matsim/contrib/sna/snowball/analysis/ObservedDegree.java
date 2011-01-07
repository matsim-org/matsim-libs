/* *********************************************************************** *
 * project: org.matsim.*
 * SampledDegree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.sna.snowball.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * 
 * A class that provides functionality to analyze degree related
 * graph-properties calculated on a snowball sample.
 * 
 * @author illenberger
 * 
 */
public class ObservedDegree extends Degree {

	private static ObservedDegree instance;

	public static ObservedDegree getInstance() {
		if (instance == null)
			instance = new ObservedDegree();
		return instance;
	}

	/**
	 * @param vertices
	 *            a set of sampled vertices
	 * 
	 * @return a descriptive statistics object containing all sampled vertices
	 *         with pi-values associated.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DescriptiveStatistics distribution(Set<? extends Vertex> vertices) {
		return super.distribution(SnowballPartitions.<SampledVertex> createSampledPartition((Set<SampledVertex>) vertices));
	}

	/**
	 * @param vertices
	 *            a set of sampled vertices
	 * @return an object-double map containing all sampled vertices and their
	 *         degree.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		return super.values(SnowballPartitions.createSampledPartition((Set<SampledVertex>) vertices));
	}

	/**
	 * Calculated the Pearson correlation coefficient of the degrees on either
	 * end of all sampled edges, i.e., edges where both vertices are sampled.
	 * Note: This implementation is different form the one in the super class
	 * (due to the estimation technique).
	 * 
	 * @param graph
	 *            a sampled graph
	 * @return the Pearson correlation coefficient of the degrees on either end
	 *         of all sampled edges.
	 */
	@Override
	public double assortativity(Graph graph) {
		TDoubleArrayList values1 = new TDoubleArrayList();
		TDoubleArrayList values2 = new TDoubleArrayList();
		for (Edge e : graph.getEdges()) {
			SampledVertex v1 = (SampledVertex) e.getVertices().getFirst();
			SampledVertex v2 = (SampledVertex) e.getVertices().getSecond();
			if (v1.isSampled() && v2.isSampled()) {
				int d_v1 = v1.getEdges().size();
				int d_v2 = v2.getEdges().size();

				values1.add(d_v1);
				values2.add(d_v2);
			}
		}

		if (values1.size() > 0) {
			return new PearsonsCorrelation().correlation(values1.toNativeArray(), values2.toNativeArray());
		} else
			return 0;
	}

}
