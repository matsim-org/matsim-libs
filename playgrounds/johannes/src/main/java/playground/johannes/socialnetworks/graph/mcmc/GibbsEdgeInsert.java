/* *********************************************************************** *
 * project: org.matsim.*
 * GibbsSampler.java
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

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;


/**
 * @author illenberger
 *
 */
public class GibbsEdgeInsert extends AbstractGibbsSampler {
	
	/**
	 * 
	 */
	public GibbsEdgeInsert() {
		super();
	}

	/**
	 * @param seed
	 */
	public GibbsEdgeInsert(long seed) {
		super(seed);
	}

	public <V extends Vertex> boolean step(AdjacencyMatrix<V> m, EnsembleProbability ensembleProba) {
		boolean accept = false;
		random.nextInt(m.getVertexCount());
		int i = random.nextInt(m.getVertexCount());
		random.nextInt(m.getVertexCount());
		int j = random.nextInt(m.getVertexCount());
		
		if(i != j) {
			boolean y_ij = m.getEdge(i, j);
			double p = 1 / (1 + ensembleProba.ratio(m, i, j, y_ij));
			
			random.nextDouble();
			if(random.nextDouble() <= p) {
				/*
				 * Switch or leave the edge on.
				 */
				if(!y_ij) {
					m.addEdge(i, j);
					
				}
				accept = true;
			} else {
				/*
				 * Switch or leave the edge off.
				 */
				if(y_ij) {
					m.removeEdge(i, j);
					
				}
				accept = false;
			}
		}
		
		return accept;
	}
	
}
