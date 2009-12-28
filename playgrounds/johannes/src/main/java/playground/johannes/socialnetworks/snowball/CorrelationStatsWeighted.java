/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeCorrelation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.snowball;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.socialnetworks.graph.EdgeDecorator;
import playground.johannes.socialnetworks.graph.GraphProjection;
import playground.johannes.socialnetworks.graph.VertexDecorator;

/**
 * @author illenberger
 *
 */
public class CorrelationStatsWeighted extends GraphPropertyEstimator {

//	private double globalResponseRate;
	
	public CorrelationStatsWeighted(String outputDir, double responseRate) {
		super(outputDir);
//		this.globalResponseRate = responseRate;
		openStatsWriters("dcorrelation-weighted");
	}

	@Override
	@SuppressWarnings("unchecked")
	public DescriptiveStatistics calculate(GraphProjection<SampledGraph, SampledVertex, SampledEdge> g, int iteration) {
		double product = 0;
		double sum = 0;
		double squareSum = 0;
		
		double product_w = 0;
		double sum_w = 0;
		double squareSum_w = 0;

		double edgesum = 0;
		double wsum = 0;
		
		double psum = 0;
		for(VertexDecorator<SampledVertex> v : g.getVertices()) {
			if(!v.getDelegate().isAnonymous())
				psum += v.getDelegate().getSampleProbability();
		}
		
		for (EdgeDecorator<SampledEdge> e : g.getEdges()) {
			VertexDecorator<SampledVertex> v1 = (VertexDecorator<SampledVertex>) e.getVertices().getFirst();
			VertexDecorator<SampledVertex> v2 = (VertexDecorator<SampledVertex>) e.getVertices().getSecond();
//			if (!v1.getDelegate().isAnonymous()
//					&& !v2.getDelegate().isAnonymous()) {
			if (v1.getDelegate().isSampled()
					&& v2.getDelegate().isSampled()) {
				int d_v1 = v1.getEdges().size();
				int d_v2 = v2.getEdges().size();

				double p1 = v1.getDelegate().getSampleProbability() / psum;
				double p2 = v2.getDelegate().getSampleProbability() / psum;
				double w = 1 / ((p1 + p2) - (p1 * p2));
//				double w = 1 / (globalResponseRate * Math.max(p1, p2));
				
				sum += 0.5 * (d_v1 + d_v2);
				squareSum += 0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2));
				product += d_v1 * d_v2;
				
				edgesum++;
				
				sum_w += 0.5 * (d_v1 + d_v2) * w;
				squareSum_w += 0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2)) * w;
				product_w += d_v1 * d_v2 * w;
				
				wsum += w;
			}
		}
		
		double norm = 1 / edgesum;
		double r = ((norm * product) - Math.pow(norm * sum, 2)) / ((norm * squareSum) - Math.pow(norm * sum, 2));
		DescriptiveStatistics obs = new DescriptiveStatistics();
		obs.addValue(r);
		
		norm = 1 / wsum;
		r = ((norm * product_w) - Math.pow(norm * sum_w, 2)) / ((norm * squareSum_w) - Math.pow(norm * sum_w, 2));
		DescriptiveStatistics estim = new DescriptiveStatistics();
		estim.addValue(r);
		
		dumpObservedStatistics(getStatisticsMap(obs), iteration);
		dumpEstimatedStatistics(getStatisticsMap(estim), iteration);
		
		return obs;
	}
}
