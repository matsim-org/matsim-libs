/* *********************************************************************** *
 * project: org.matsim.*
 * Ergm.java
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
public class Ergm implements ConditionalDistribution {

	private ErgmTerm[] ergmTerms;
	
	public void setErgmTerms(ErgmTerm[] terms) {
		ergmTerms = terms;
	}
	
	public double evaluate(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		return evaluateExpHamiltonian(m, i, j, y_ij);
	}

	public double evaluateExpHamiltonian(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		double sum = 0;
		for(ErgmTerm term : ergmTerms)
			sum += term.evaluate(m, i, j, y_ij);
		return Math.exp(sum);
	}
	
	public static void main(String[] args) {
		ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		PlainGraph graph = generator.generate(100, 0.1, 0);
		
		dump(graph);
		
		AdjacencyMatrix m = new AdjacencyMatrix(graph);
		
		GibbsSampler sampler = new GibbsSampler(0);
		
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[2];
		
		double alpha1 = 0.1;
		double theta1 = Math.log(alpha1/(1-alpha1));
		terms[0] = new ErgmDensity();
		terms[0].setTheta(theta1);
		
		double alpha2 = 0.5;
		double theta2 = Math.log(alpha2/(1-alpha2));
		terms[1] = new ErgmTwoStars();
		terms[1].setTheta(theta2);
		
		ergm.setErgmTerms(terms);
		
		sampler.sample(m, ergm, (int)1E7);
		
		graph = m.getGraph(new PlainGraphFactory());
		
		dump(graph);
	}

	private static void dump(Graph g) {
		System.out.println(String.format("m=%1$s, <k>=%2$s, c=%3$s, 2-stars=%4$s",
				g.getEdges().size(),
				GraphStatistics.getDegreeStatistics(g).getMean(),
				GraphStatistics.getClusteringStatistics(g).getMean(),
				GraphStatistics.getNumTwoStars(g)));
	}
}
