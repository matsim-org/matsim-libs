/* *********************************************************************** *
 * project: org.matsim.*
 * KMLSeedComponents.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.ColorUtils;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.snowball2.spatial.io.KMLSampledComponents;

/**
 * @author illenberger
 *
 */
public class KMLSeedComponents extends KMLSampledComponents {

	private Map<Vertex, Color> colors = new HashMap<Vertex, Color>();
	
	@Override
	public Color getColor(Object object) {
		return colors.get(object);
	}

	@Override
	public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
		List<Set<? extends SpatialVertex>> partitions = super.getPartitions(graph);
		
		
		for(Set<? extends SpatialVertex> partition : partitions) {
			Map<SampledVertex, Color> sourceColors = new HashMap<SampledVertex, Color>();
//			Map<SampledSocialVertex, SampledSocialVertex> sourceMapping = new HashMap<SampledSocialVertex, SampledSocialVertex>();
			for(SpatialVertex vertex : partition) {
				SampledVertex seed = ((SampledVertex)vertex).getSeed();
				Color col = sourceColors.get(seed);
				if(col == null) {
					col = ColorUtils.getGRBColor(sourceColors.size()/5.0 + 0.1);
					sourceColors.put(seed, col);
					}
				colors.put(vertex, col);
			}
			
			for(SpatialVertex vertex : partition) {
				Set<Vertex> seeds = new HashSet<Vertex>();
				for(Vertex neighbour : vertex.getNeighbours()) {
					seeds.add(((SampledVertex)neighbour).getSeed());
				}
				
				if(seeds.size() > 1) {
					colors.put(vertex, Color.WHITE);
//					System.err.println(((SampledSocialVertex)vertex).getPerson().getId().toString());
				}
			}
			
//			StringBuilder builder = new StringBuilder();
//			for(SampledSocialVertex vertex : sourceColors.keySet()) {
//				builder.append(vertex.getPerson().getId().toString());
//				builder.append(" ");
//			}
//			
//			System.out.println(String.format("Component n=%1$s has seeds %2$s", partition.size(), builder.toString()));
		}
		return partitions;
	}

//	private SampledSocialVertex getSource(SampledSocialVertex vertex) {
//		if(vertex.getSources().isEmpty())
//			return vertex;
//		else
//			return getSource(vertex.getSources().get(0));
//	}
}
