/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeSampled.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.snowball2;

import java.util.HashMap;
import java.util.Set;

import playground.johannes.snowball.Histogram;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * @author illenberger
 * 
 */
public class DegreeSampled extends Degree {

	private boolean biasCorrection;
	
	@Override
	public double run(Graph g) {
		if (g instanceof SampledGraph) {
			values = new HashMap<Vertex, Integer>();
			int sum = 0;
			double wsum = 0;
			Set<SampledVertex> vertices = ((SampledGraph) g).getVertices();
			double weight = 1;
			for (SampledVertex v : vertices) {
				if (!v.isAnonymous()) {
					if(biasCorrection)
						weight = 1 / v.getSampleProbability();
					
					sum += v.degree() * weight;
					wsum += weight;
					values.put(v, v.degree());
				}
			}
			return sum / wsum;
		} else {
			throw new IllegalArgumentException(
					"Graph must be an instance of SampledGrah!");
		}
	}

	@Override
	protected void fillHistogram(Histogram histogram) {
		for(Vertex v : values.keySet()) {
			histogram.add(v.degree(), 1 / ((SampledVertex)v).getSampleProbability());
		}
	}
	
	public void setBiasCorrection(boolean flag) {
		biasCorrection = flag;
	}
}
