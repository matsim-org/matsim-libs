/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatedDegree.java
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
package org.matsim.contrib.socnetgen.sna.snowball.analysis;

import gnu.trove.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Degree;
import org.matsim.contrib.socnetgen.sna.math.DescriptivePiStatisticsFactory;
import org.matsim.contrib.socnetgen.sna.snowball.SampledEdge;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;

import java.util.Set;


/**
 * A class that provides functionality to estimate degree related
 * graph-properties on a snowball sample.
 * 
 * @author illenberger
 *
 */
public class EstimatedDegree extends Degree {

	private static final Logger logger = Logger.getLogger(EstimatedDegree.class);
	
	private PiEstimator piEstimator;
	
	private DescriptivePiStatisticsFactory factory;

	/**
	 * Creates new estimated degree object configured with <tt>estimator</tt> as
	 * pi-estimator and <tt>factory</tt> as the factory for creating new
	 * instances of {@link DescriptivePiStatistics} which does the actual
	 * estimation of degree properties.
	 * 
	 * @param estimator a pi-estimator
	 * @param factory a factory for creating new instance of {@link DescriptivePiStatistics}.
	 */
	public EstimatedDegree(PiEstimator estimator, DescriptivePiStatisticsFactory factory) {
		this.piEstimator = estimator;
		this.factory = factory;
	}
	
	/**
	 * Returns a descriptive statistics object containing all sampled vertices
	 * each associated a pi-value.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @return a descriptive statistics object.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DescriptivePiStatistics statistics(Set<? extends Vertex> vertices) {
		DescriptivePiStatistics stats = factory.newInstance();
	
		int p0 = 0;
		
		Set<SampledVertex> samples = SnowballPartitions.<SampledVertex>createSampledPartition((Set<SampledVertex>)vertices);
	
		for(SampledVertex vertex : samples) {
			double p = piEstimator.probability(vertex);
			if(p > 0) {
				stats.addValue(vertex.getNeighbours().size(), p);
			} else
				p0++;
		}
		
		if(p0 > 0)
			logger.warn(String.format("There are %1$s vertices with probability 0!", p0));
		
		return stats;
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
		return super.values(SnowballPartitions.createSampledPartition((Set<SampledVertex>)vertices));
	}

	/**
	 * Estimates the degree-degree correlation of a sampled graph.
	 * 
	 * @param g
	 *            a sampled graph
	 * @return the estimated degree-degree correlation.
	 */
	public double assortativity(Graph g) {
		SampledGraph graph = (SampledGraph) g;
		int iteration = SnowballStatistics.getInstance().lastIteration(graph.getVertices());
		if(iteration == 0)
			return Double.NaN;
		
		double product = 0;
		double sum = 0;
		double squareSum = 0;
		double M_hat = 0;
		for (SampledEdge e : graph.getEdges()) {
			SampledVertex v_i = e.getVertices().getFirst();
			SampledVertex v_j = e.getVertices().getSecond();
			if (v_i.isSampled() && v_j.isSampled()) {
				double p_i = piEstimator.probability(v_i, iteration - 1);
				double p_j = piEstimator.probability(v_j, iteration - 1);
				if (p_i > 0 && p_j > 0) {
					double p = (p_i + p_j) - (p_i * p_j);
					
					int k_i = v_i.getEdges().size();
					int k_j = v_j.getEdges().size();
					
					sum += 0.5 * (k_i + k_j) / p;
					squareSum += 0.5 * (Math.pow(k_i, 2) + Math.pow(k_j, 2))/p;
					product += k_i * k_j/p;
					
					M_hat += 1/p;
				}
			}
			
		}

		double norm = 1 / M_hat;
		return ((norm * product) - Math.pow(norm * sum, 2)) / ((norm * squareSum) - Math.pow(norm * sum, 2));
		
//		double M = 0;
//
//		double t_ij = 0;
//		double t_ii = 0;
//		double t_jj = 0;
//		double t_i = 0;
//		double t_j = 0;
//
//		for (SampledEdge edge : graph.getEdges()) {
//			SampledVertex v_i = edge.getVertices().getFirst();
//			SampledVertex v_j = edge.getVertices().getSecond();
//
//			if (v_i.isSampled() && v_j.isSampled()) {
//				double p_i = piEstimator.probability(v_i, iteration - 1);
//				double p_j = piEstimator.probability(v_j, iteration - 1);
//				if (p_i > 0 && p_j > 0) {
//					double p = (p_i + p_j) - (p_i * p_j);
//					double k_i = v_i.getNeighbours().size();
//					double k_j = v_j.getNeighbours().size();
//
//					M += 1 / p;
//
//					t_ij += k_i * k_j / p;
//					t_ii += k_i * k_i / p;
//					t_jj += k_j * k_j / p;
//					t_i += k_i / p;
//					t_j += k_j / p;
//				}
//			}
//		}
//
//		double S_ij = (1 / (M - 1) * t_ij) - (1 / (M * (M - 1)) * t_i * t_j);
//		double S_ii = (1 / (M - 1) * t_ii) - (1 / (M * (M - 1)) * t_i * t_i);
//		double S_jj = (1 / (M - 1) * t_jj) - (1 / (M * (M - 1)) * t_j * t_j);
//
//		return S_ij / (Math.sqrt(S_ii) * Math.sqrt(S_jj));
	}

}
