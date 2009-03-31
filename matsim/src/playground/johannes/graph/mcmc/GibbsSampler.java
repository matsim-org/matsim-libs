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
package playground.johannes.graph.mcmc;

import java.util.Random;


/**
 * @author illenberger
 *
 */
public class GibbsSampler {

	private Random random;
	
	public GibbsSampler() {
		random = new Random();
	}
	
	public GibbsSampler(long seed) {
		random = new Random(seed);
	}
	
	public void sample(AdjacencyMatrix m, ConditionalDistribution d, int burninTime) {
		int N = m.getVertexCount();
		for(int it = 0; it < burninTime; it++) {
			int i = random.nextInt(N);
			int j = random.nextInt(N);
			
			if(i != j) {
				boolean y_ij = m.getEdge(i, j);
				double p = 1 / (1 + d.evaluateChange_1(m, i, j, y_ij));
				
				if(random.nextDouble() <= p) {
					/*
					 * Switch or leave the edge on.
					 */
					if(!y_ij) {
						m.addEdge(i, j);
					}
				} else {
					/*
					 * Switch or leave the edge off.
					 */
					if(y_ij) {
						m.removeEdge(i, j);
					}
				}
			}
			
			if(it%100000==0) {
				System.out.println(it + " steps simulated.");
				int sum = 0;
				for(int k = 0; k < m.getVertexCount(); k++) {
					sum += m.getNeighbours(k).size();
				}
				System.out.println("Mean degree is " + (sum/(float)m.getVertexCount()));
			}
		}
	}
}
