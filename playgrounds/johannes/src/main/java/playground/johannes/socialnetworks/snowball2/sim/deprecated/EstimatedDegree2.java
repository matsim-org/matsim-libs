/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatedDegree2.java
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
package playground.johannes.socialnetworks.snowball2.sim.deprecated;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.math.DescriptivePiStatisticsFactory;
import org.matsim.contrib.sna.snowball.SampledEdge;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.EstimatedDegree;
import org.matsim.contrib.sna.snowball.analysis.PiEstimator;

import playground.johannes.socialnetworks.snowball2.sim.SampleStats;

/**
 * @author illenberger
 * 
 */
public class EstimatedDegree2 extends EstimatedDegree {

	private PiEstimator piEstimator;
	/**
	 * @param estimator
	 * @param vertexEstimator
	 * @param edgeEstimator
	 */
	public EstimatedDegree2(PiEstimator estimator, DescriptivePiStatisticsFactory factory) {
		super(estimator, factory);
		this.piEstimator = estimator;
	}

	@Override
	public double assortativity(Graph g) {
		SampledGraph graph = (SampledGraph) g;
		SampleStats stats = new SampleStats(graph);
		
		double M = 0;

		double t_ij = 0;
		double t_ii = 0;
		double t_jj = 0;
		double t_i = 0;
		double t_j = 0;

		for (SampledEdge edge : graph.getEdges()) {
			SampledVertex v_i = edge.getVertices().getFirst();
			SampledVertex v_j = edge.getVertices().getSecond();

			if (v_i.isSampled() && v_j.isSampled()) {
//				double p = 0;
//				int it_i = v_i.getIterationSampled();
//				int it_j = v_j.getIterationSampled();
//				if(it_i > it_j) {
//					p = biasedDistribution.getProbability(v_i);
//				} else if(it_i < it_j) {
//					p = biasedDistribution.getProbability(v_j);
//				} else {
					double p_i = piEstimator.probability(v_i, stats.getMaxIteration() - 1);
					double p_j = piEstimator.probability(v_j, stats.getMaxIteration() - 1);
//					p = Math.max(p_i, p_j);
//				}
//				if(p > 0) {
				if (p_i > 0 && p_j > 0) {
					double p = (p_i + p_j) - (p_i * p_j);
//					double p = (p_i * p_j);
					double k_i = v_i.getNeighbours().size();
					double k_j = v_j.getNeighbours().size();

					M += 1 / p;

					t_ij += k_i * k_j / p;
					t_ii += k_i * k_i / p;
					t_jj += k_j * k_j / p;
					t_i += k_i / p;
					t_j += k_j / p;
				}
			}
		}

		double S_ij = (1 / (M - 1) * t_ij) - (1 / (M * (M - 1)) * t_i * t_j);
		double S_ii = (1 / (M - 1) * t_ii) - (1 / (M * (M - 1)) * t_i * t_i);
		double S_jj = (1 / (M - 1) * t_jj) - (1 / (M * (M - 1)) * t_j * t_j);

		return S_ij / (Math.sqrt(S_ii) * Math.sqrt(S_jj));
	}

}
