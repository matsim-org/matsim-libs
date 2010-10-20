/* *********************************************************************** *
 * project: org.matsim.*
 * ApplySeedsFilter.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;

/**
 * @author illenberger
 *
 */
public class ApplySeedsFilter implements GraphFilter<SampledGraph> {

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.graph.analysis.GraphFilter#apply(org.matsim.contrib.sna.graph.Graph)
	 */
	@Override
	public SampledGraph apply(SampledGraph graph) {
		Set<SampledVertex> neighbors = new HashSet<SampledVertex>();
		neighbors.addAll(SnowballPartitions.createSampledPartition(graph.getVertices(), 0));
		
		for(SampledVertex vertex : neighbors) {
			vertex.setSeed(vertex);
		}
		
		while(!neighbors.isEmpty()) {
			Set<SampledVertex> newNeighbors = new HashSet<SampledVertex>();
			for(SampledVertex vertex : neighbors) {
				expand(vertex, newNeighbors);
			}
			
			neighbors = newNeighbors;
		}
		
		return graph;
	}

	private void expand(SampledVertex ego, Set<SampledVertex> neighbors) {
		int it = ego.getIterationSampled();
		for(SampledVertex neighbor : ego.getNeighbours()) {
			if(neighbor.getIterationDetected() == it) {
				if(neighbor.getSeed() == null)
					neighbor.setSeed(ego.getSeed());
				if(neighbor.isSampled())
					neighbors.add(neighbor);
			} else {
				System.out.println(String.format("it=%1$s, neighbor=%2$s, vertex=%3$s", it, neighbor.toString(), ego.toString()));
			}
		}
	}
}
