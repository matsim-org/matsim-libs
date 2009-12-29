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
package playground.johannes.snowball2;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 *
 */
public class DegreeCorrelation extends GraphStatistic {

	public DegreeCorrelation(String outputDir) {
		super(outputDir);
	}

	public DescriptiveStatistics calculate(Graph g, int iteration,
			DescriptiveStatistics reference) {
		double product = 0;
		double sum = 0;
		double wdegreeSum = 0;
		double squareSum = 0;
//		double wsquareSum = 0;
		double edges = 0;
		
		boolean sampled = false;
		if(g instanceof SampledGraph)
			sampled = true;
		
		/*
		 * Calculate normalization constant
		 */
		double wconst = 1;
		if(sampled) {
			double wsum = 0;
			int count = 0;
			for(Object v : g.getVertices()) {
				if (!((SampledVertex)v).isAnonymous()) {
					wsum += 1/((SampledVertex)v).getSampleProbability();
					count++;
				}
			}
			wconst = count/wsum;
		}
		
		double probaSum = 0;
		double probaSquareSum = 0;
		double probaProductSum = 0;
		for (Object e : g.getEdges()) {
			Pair p = ((Edge) e).getEndpoints();
			Vertex v1 = (Vertex) p.getFirst();
			Vertex v2 = (Vertex) p.getSecond();
		
			if(sampled) {
//				if (!((SampledVertex)v1).isAnonymous() && !((SampledVertex)v2).isAnonymous()) {
//					int d_v1 = v1.degree();
//					int d_v2 = v2.degree();
////					double d_v1 = v1.degree() * 1/((SampledVertex)v1).getSampleProbability();// * wconst;
////					double d_v2 = v2.degree() * 1/((SampledVertex)v2).getSampleProbability();// * wconst;
//
//					sum += d_v1 + d_v2;
////					wdegreeSum += (d_v1 * 1/((SampledVertex)v1).getSampleProbability() * wconst)
////								+ (d_v2 * 1/((SampledVertex)v2).getSampleProbability() * wconst);
//					squareSum += Math.pow(d_v1, 2) + Math.pow(d_v2, 2);
////					wsquareSum += Math.pow(d_v1 * 1/((SampledVertex)v1).getSampleProbability() * wconst, 2)
////								+ Math.pow(d_v2 * 1/((SampledVertex)v2).getSampleProbability() * wconst, 2);
////					product += d_v1 * 1/((SampledVertex)v1).getSampleProbability() * wconst
////							 * d_v2 * 1/((SampledVertex)v2).getSampleProbability() * wconst;
//					product += d_v1 * d_v2;
//					
//					edges++;
//				}
				
				if (!((SampledVertex)v1).isAnonymous() && !((SampledVertex)v2).isAnonymous()) {
					int d_v1 = v1.degree();
					int d_v2 = v2.degree();
					double p_v1 = ((SampledVertex)v1).getSampleProbability();// * wconst;
					double p_v2 = ((SampledVertex)v2).getSampleProbability();// * wconst;
					double proba = p_v1 * p_v2;
					
					sum += 0.5 * (d_v1 + d_v2) /proba;
					probaSum += 1/proba;
					
					squareSum += 0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2)) / proba;
					probaSquareSum = 1/proba;
					
					product += (d_v1 * d_v2)/proba;
					probaProductSum += 1/proba;
					
					edges++;
				}
			} else {
				int d_v1 = v1.degree();
				int d_v2 = v2.degree();

				sum += 0.5 * (d_v1 + d_v2);
				wdegreeSum = sum;
				squareSum += 0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2));
				product += d_v1 * d_v2;
				
				probaSum++;
				probaSquareSum++;
				probaProductSum++;
				edges++;
			}
		}
		double M_minus1 = 1 / (double) edges;
		double normSumSquare = Math.pow((1/probaSum * sum), 2);
//		double wSumSquare = Math.pow((M_minus1 * 0.5 * wdegreeSum), 2);
		double numerator = (1/probaProductSum * product) - normSumSquare;
		double denumerator = (1/probaSquareSum * squareSum) - normSumSquare;

		double result = numerator / denumerator;
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.addValue(result);
		return stats;
	}
}
