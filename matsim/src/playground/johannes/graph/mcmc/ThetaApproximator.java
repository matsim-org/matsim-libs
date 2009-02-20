/* *********************************************************************** *
 * project: org.matsim.*
 * ThetaApproximator.java
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

/**
 * 
 */
package playground.johannes.graph.mcmc;

import playground.johannes.graph.Graph;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.generators.ErdosRenyiGenerator;
import playground.johannes.graph.generators.PlainGraphFactory;

/**
 * @author illenberger
 *
 */
public class ThetaApproximator {

	private static final int burninTime = (int)1E6;
	
	private static final double c_target = 0.4;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		PlainGraph graph = generator.generate(100, 0.1, 0);
		
		dump(graph);
		
		GibbsSampler sampler = new GibbsSampler(0);
		
		double alpha1 = 0.9;
		double alpha2 = 0.9;
		double alpha3 = 0.9;
		
		
		
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[3];
		
		terms[0] = new ErgmDensity();
		double theta0 = Math.log(alpha1/(1-alpha1));
		terms[0].setTheta(theta0);
		
		terms[1] = new ErgmTwoStars();
		double theta1 = Math.log(alpha2 /(1 - alpha2));
		terms[1].setTheta(theta1);
		
		AdjacencyMatrix m = new AdjacencyMatrix(graph);
		terms[2] = new ErgmTriangles();
		double theta3 = Math.log(alpha3/(1- alpha3 ));
		
		ergm.setErgmTerms(terms);
		
		double delta = 1;
		double delta_max = 0.05;
		double theta2_current;
//		double theta2_old = theta2 + 2;
		double theta2_max = Double.POSITIVE_INFINITY;
		double theta2_min = Double.NEGATIVE_INFINITY;
		
		System.out.println("Starting sampling...");
		int i = 0;
		while(Math.abs(delta) > delta_max) {
			m = new AdjacencyMatrix(graph);
			
			
			terms[2].setTheta(theta3);
			theta2_current = theta3;
		
			
			long time = System.currentTimeMillis();
			try {
				sampler.sample(m, ergm, burninTime);
			} catch (RuntimeException e) {
				System.err.println("Reached density 0.5");
			}
			time = System.currentTimeMillis() - time;
			graph = m.getGraph(new PlainGraphFactory());
			double c = GraphStatistics.getClusteringStatistics(graph).getMean();
			
			delta = c_target - c;
			if(delta < 0) {
				// c is too big
				theta2_max = theta3;
				if(Double.isInfinite(theta2_min))
					theta3 = theta2_current - 1;
				else
					theta3 = theta2_current - ((theta2_current - theta2_min) * 0.5);
			} else {
				// c is too small
				theta2_min = theta3;
				if(Double.isInfinite(theta2_max))
					theta3 = theta2_current + 1;
				else
					theta3 = theta2_current + (theta2_max - theta2_current) * 0.5;
			}
//			theta2_old = theta2_current;
			dump(graph);
			System.out.println(String.format("[%1$s - %2$s ms] c_target=%3$s, theta_old=%4$s. theta_new=%5$s", i, time, c_target, theta2_current, theta3));
			i++;
		}
		
		
//		graph = m.getGraph(new PlainGraphFactory());
		
	}

	private static void dump(Graph g) {
		System.out.println(String.format("m=%1$s, <k>=%2$s, c=%3$s, 2-stars=%4$s",
				g.getEdges().size(),
				GraphStatistics.getDegreeStatistics(g).getMean(),
				GraphStatistics.getClusteringStatistics(g).getMean(),
				GraphStatistics.getNumTwoStars(g)));
	}
}
