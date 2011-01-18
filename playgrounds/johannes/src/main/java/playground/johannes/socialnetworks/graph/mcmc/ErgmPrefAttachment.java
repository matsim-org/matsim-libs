/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmPrefAttachment.java
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
package playground.johannes.socialnetworks.graph.mcmc;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class ErgmPrefAttachment implements GraphProbability {

	@Override
	public <V extends Vertex> double difference(AdjacencyMatrix<V> y, int i, int j, boolean yIj) {
		int k_minus = y.getNeighborCount(j);
		if(yIj)
			k_minus--;
		
		int k_plus = k_minus + 1;
		
		return Math.exp(-k_minus + k_plus);
	}

}
