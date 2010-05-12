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

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;


/**
 * @author illenberger
 *
 */
public class GibbsSampler {
	
	private static final Logger logger = Logger.getLogger(GibbsSampler.class);

	protected Random random;
	
	private int inteval = 100000;
	
	public GibbsSampler() {
		random = new Random();
	}
	
	public GibbsSampler(long seed) {
		random = new Random(seed);
	}
	
	public void setInterval(int interval) {
		this.inteval = interval;
	}
	
	public <V extends Vertex> void sample(AdjacencyMatrix<V> y, GraphProbability d, SampleHandler<V> handler) {
		long time = System.currentTimeMillis();
		
		int accept = 0;
		long it = 0;
		while(handler.handle(y, it)) {
			it++;
			if(step(y, d))
				accept++;
			
			if(it % inteval == 0) {
				logger.info(String.format("[%1$s] Accepted %2$s of %3$s steps (ratio = %4$s).", it, accept, inteval, accept/(float)inteval));
				accept = 0;
			}
		}
				
		logger.info(String.format("Sampling done in %1$s s.", (System.currentTimeMillis() - time)/1000));
	}
	
	public <V extends Vertex> boolean step(AdjacencyMatrix<V> m, GraphProbability d) {
		boolean accept = false;
		int i = random.nextInt(m.getVertexCount());
		int j = random.nextInt(m.getVertexCount());
		
		if(i != j) {
			boolean y_ij = m.getEdge(i, j);
			double p = 1 / (1 + d.difference(m, i, j, y_ij));
			
			if(random.nextDouble() <= p) {
				/*
				 * Switch or leave the edge on.
				 */
				if(!y_ij) {
					m.addEdge(i, j);
					accept = true;
//					d.addEdge(m, i, j);
				}
			} else {
				/*
				 * Switch or leave the edge off.
				 */
				if(y_ij) {
					m.removeEdge(i, j);
					accept = true;
//					d.removeEdge(m, i, j);
				}
			}
		}
		
		return accept;
	}
	
}
