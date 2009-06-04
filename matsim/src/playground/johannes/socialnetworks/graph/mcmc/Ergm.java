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


/**
 * @author illenberger
 *
 */
public class Ergm implements ConditionalDistribution {

	private ErgmTerm[] ergmTerms;
	
	public void setErgmTerms(ErgmTerm[] terms) {
		ergmTerms = terms;
	}
	
	public ErgmTerm[] getErgmTerms() {
		return ergmTerms;
	}
	

	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean y_ij) {
		return evaluateExpHamiltonian(y, i, j, y_ij);
	}

	public double evaluateExpHamiltonian(AdjacencyMatrix y, int i, int j, boolean y_ij) {
		double sum = 0;
		for(ErgmTerm term : ergmTerms)
			sum += term.changeStatistic(y, i, j, y_ij);
		return Math.exp(sum);
	}
}
