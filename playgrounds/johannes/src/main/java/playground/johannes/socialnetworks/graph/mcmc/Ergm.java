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
package playground.johannes.socialnetworks.graph.mcmc;

import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;



/**
 * @author illenberger
 *
 */
public class Ergm implements ConditionalDistribution {

	private ErgmTerm[] ergmTerms;
	
//	private TIntDoubleHashMap norm_i = new TIntDoubleHashMap();
	
	public void setErgmTerms(ErgmTerm[] terms) {
		ergmTerms = terms;
	}
	
	public ErgmTerm[] getErgmTerms() {
		return ergmTerms;
	}
	

	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean y_ij) {
		double h_y = evaluateExpHamiltonian(y, i, j, y_ij);
//		if(Double.isInfinite(h_y))
//			throw new IllegalArgumentException("H(y) must not be infinity!");
		if(Double.isNaN(h_y))
			throw new IllegalArgumentException("H(y) must not be NaN!");
		
		
		return h_y;
	}

	public double evaluateExpHamiltonian(AdjacencyMatrix y, int i, int j, boolean y_ij) {
		double sum = 0;
		for(ErgmTerm term : ergmTerms) {
			sum += term.changeStatistic(y, i, j, y_ij); 
		}
		return Math.exp(sum);
	}

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.graph.mcmc.ConditionalDistribution#addEdge(playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix, int, int)
	 */
	public void addEdge(AdjacencyMatrix y, int i, int j) {
		for(ErgmTerm term : ergmTerms) {
			term.addEdge(y, i, j);
		}
		
	}

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.graph.mcmc.ConditionalDistribution#removeEdge(playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix, int, int)
	 */
	public void removeEdge(AdjacencyMatrix y, int i, int j) {
		for(ErgmTerm term : ergmTerms) {
			term.removeEdge(y, i, j);
		}
		
	}
	
}
