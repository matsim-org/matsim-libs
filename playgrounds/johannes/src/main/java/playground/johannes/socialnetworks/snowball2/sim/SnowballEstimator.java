/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballEstimator.java
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
package playground.johannes.socialnetworks.snowball2.sim;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class SnowballEstimator {

	private double share;
	
	public void update(SampledGraph graph, int iteration) {
		int count = 0;
		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex) vertex).isSampled() && ((SampledVertex) vertex).getIterationSampled() < iteration)
				count++;
		}
	}
	
	public double getProbability(SampledVertex vertex) {
		return 1 - Math.pow(1 - share, vertex.getNeighbours().size());
	}
}
