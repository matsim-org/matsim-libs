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

import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class DegreeSequence implements EdgeProbabilityFunction {

	private final int N;
	
	private final int[] kSequence;
	
	public DegreeSequence(AdjacencyMatrix<?> y) {
		N = y.getVertexCount();
		
		kSequence = new int[N];
		for(int i = 0; i < N; i++)
			kSequence[i] = y.getNeighborCount(i);
	}
	
	@Override
	public double probability(int i, int j) {
		return kSequence[i]/(double)N;
	}

}
