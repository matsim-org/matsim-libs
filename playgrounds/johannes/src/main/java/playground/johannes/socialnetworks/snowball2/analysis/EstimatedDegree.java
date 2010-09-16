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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;

import playground.johannes.socialnetworks.snowball2.sim.deprecated.PopulationEstimator;
import playground.johannes.socialnetworks.statistics.EstimatedDistribution;

/**
 * @author illenberger
 *
 */
public class EstimatedDegree extends Degree {

	private static final Logger logger = Logger.getLogger(EstimatedDegree.class);
	
	private ProbabilityEstimator biasedDistribution;
	
	private PopulationEstimator vertexEstimator;
	
	private PopulationEstimator edgeEstimator;
	
	public EstimatedDegree(ProbabilityEstimator estimator, PopulationEstimator vertexEstimator, PopulationEstimator edgeEstimator) {
		this.biasedDistribution = estimator;
		this.vertexEstimator = vertexEstimator;
		this.edgeEstimator = edgeEstimator;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Distribution distribution(Set<? extends Vertex> vertices) {
		Distribution distr = new EstimatedDistribution(vertexEstimator);
	
		int cntk0 = 0;
		
		Set<SampledVertex> samples = SnowballPartitions.<SampledVertex>createSampledPartition((Set<SampledVertex>)vertices);
		for(SampledVertex vertex : samples) {
			double p = biasedDistribution.getProbability(vertex);
			if(p > 0) {
				distr.add(vertex.getNeighbours().size(), 1/p);
			} else
				cntk0++;
		}
		
		if(cntk0 > 0)
			logger.warn(String.format("There are %1$s vertices with probability 0.!", cntk0));
		
		return distr;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TObjectDoubleHashMap<Vertex> values(Collection<? extends Vertex> vertices) {
		return (TObjectDoubleHashMap<Vertex>) super.values(SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>) vertices));
	}

	@Override
	public double assortativity(Graph graph) {
		EstimatedDistribution product = new EstimatedDistribution(edgeEstimator);
		EstimatedDistribution sum = new EstimatedDistribution(edgeEstimator);
		EstimatedDistribution squareSum = new EstimatedDistribution(edgeEstimator);
		int edgecount = 0;
		for (Edge e : graph.getEdges()) {
			SampledVertex v1 = (SampledVertex) e.getVertices().getFirst();
			SampledVertex v2 = (SampledVertex) e.getVertices().getSecond();
			if(v1.isSampled() && v2.isSampled()) {
				int d_v1 = v1.getEdges().size();
				int d_v2 = v2.getEdges().size();

				double p_1 = biasedDistribution.getProbability(v1);
				double p_2 = biasedDistribution.getProbability(v2);
				
				if (p_1 > 0 && p_2 > 0) {
					double p = (p_1 + p_2) - (p_1 * p_2);

					sum.add(0.5 * (d_v1 + d_v2), 1 / p);
					squareSum.add(0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2)), 1 / p);
					product.add(d_v1 * d_v2, 1 / p);

					edgecount++;
				}
			}
		}
		
		return ((product.mean()) - Math.pow(sum.mean(), 2)) / ((squareSum.mean()) - Math.pow(sum.mean(), 2));
	}

}
