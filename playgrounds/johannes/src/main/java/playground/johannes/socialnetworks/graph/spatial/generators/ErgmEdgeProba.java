/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmEdgeProba.java
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;

/**
 * @author illenberger
 * 
 */
public class ErgmEdgeProba extends ErgmTerm {

	private final EdgeProbabilityFunction probaFunction;

	public ErgmEdgeProba(EdgeProbabilityFunction function) {
		probaFunction = function;
	}

	@Override
	public <V extends Vertex> double difference(AdjacencyMatrix<V> y, int i, int j, boolean yIj) {
		double p = probaFunction.probability(i, j);

		if(p == 0)
			return Double.POSITIVE_INFINITY;
		else
			return (1 - p) / p;
	}
}
