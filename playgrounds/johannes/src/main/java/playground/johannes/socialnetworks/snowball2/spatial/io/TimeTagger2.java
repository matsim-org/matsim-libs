/* *********************************************************************** *
 * project: org.matsim.*
 * TimeTagger2.java
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
package playground.johannes.socialnetworks.snowball2.spatial.io;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.EdgeDecorator;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;


/**
 * @author illenberger
 *
 */
public class TimeTagger2 {

	private Map<Object, String> map;
	
	public TimeTagger2(SampledGraph graph) {
		map = new HashMap<Object, String>();
		
		List<Set<SampledVertex>> partitions = SnowballPartitions.createSampledPartitions((Set<SampledVertex>)graph.getVertices());
		
		for(SampledVertex vertex : partitions.get(0)) {
			map.put(((VertexDecorator<?>) vertex).getDelegate(), String.valueOf(0));
		}
		
		int globalTime = 1;
		for(int i = 0; i < partitions.size(); i++) {
			int kMax = 0;
			for(SampledVertex vertex : partitions.get(i)) {
				kMax = Math.max(kMax, vertex.getNeighbours().size());
			}
			
			for(SampledVertex vertex : partitions.get(i)) {
				int k = vertex.getNeighbours().size();
				int step = (int) Math.floor(kMax/(double)k);
				int time = globalTime;
				for(Edge edge : vertex.getEdges()) {
					
					if(!map.containsKey(((EdgeDecorator<?>) edge).getDelegate()))
						map.put(((EdgeDecorator<?>) edge).getDelegate(), String.valueOf(time));
					
					VertexDecorator<?> v = (VertexDecorator<?>) edge.getOpposite(vertex);
					if(!map.containsKey(v.getDelegate()))
						map.put(v.getDelegate(), String.valueOf(time));
					
					time += step;
				}
			}
			
			globalTime += kMax;
		}
	
	}
	
	public Map<?, String> getTimeTags() {
		return map;
	}
}
