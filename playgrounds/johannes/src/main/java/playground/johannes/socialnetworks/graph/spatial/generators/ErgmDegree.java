/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeSequence.java
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
public class ErgmDegree extends ErgmTerm {

	private final int N;
	
	private final int[] kSequence;
	
	public ErgmDegree(AdjacencyMatrix<?> y) {
		N = y.getVertexCount();
		
		kSequence = new int[N];
		for(int i = 0; i < N; i++)
			kSequence[i] = y.getNeighborCount(i);
	}
	
	public ErgmDegree(int[] sequence) {
		kSequence = sequence;
		N = kSequence.length;
	}

	@Override
	public <V extends Vertex> double ratio(AdjacencyMatrix<V> y, int i, int j, boolean y_ij) {
		double p = kSequence[i]/(double)N * kSequence[j]/(double)N;
		p = Math.sqrt(p);
		
		if(p == 0)
			return Double.POSITIVE_INFINITY;
		else if(Double.isInfinite(p))
			return 0;
		else {
			return (1 - p) / p;
		}
	}

}
