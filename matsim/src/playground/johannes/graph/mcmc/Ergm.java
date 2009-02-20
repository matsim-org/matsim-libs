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
	
	public double evaluate(AdjacencyMatrix m, int i, int j) {
		return evaluateExpHamiltonian(m, i, j);
	}

	public double evaluateExpHamiltonian(AdjacencyMatrix m, int i, int j) {
		double sum = 0;
		for(ErgmTerm term : ergmTerms)
			sum += term.evaluate(m, i, j);
		return Math.exp(sum);
	}
	
	public static void main(String[] args) {
		ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		PlainGraph graph = generator.generate(500, 0.01, 0);
		System.out.println("edges=" + graph.getEdges().size());
		AdjacencyMatrix m = new AdjacencyMatrix(graph);
		
		GibbsSampler sampler = new GibbsSampler(0);
		
		double theta1 = Math.log(0.05/(1-0.05));
//		double theta2;
		
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[1];
		terms[0] = new ErgmDensity();
		terms[0].setTheta(theta1);
		ergm.setErgmTerms(terms);
		
		sampler.sample(m, ergm, (int)1E6);
		
		graph = m.getGraph(new PlainGraphFactory());
		
		System.out.println("edges=" + graph.getEdges().size());
	}

}
