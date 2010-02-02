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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.ColorUtils;

import playground.johannes.socialnetworks.snowball2.spatial.io.KMLSampledComponents;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialVertex;

/**
 * @author illenberger
 *
 */
public class KMLSeedComponents extends KMLSampledComponents {

	private Map<Vertex, Color> colors = new HashMap<Vertex, Color>();
	
	@Override
	public Color getColor(Vertex object) {
		return colors.get(object);
	}

	@Override
	public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
		List<Set<? extends SpatialVertex>> partitions = super.getPartitions(graph);
		
		
		for(Set<? extends SpatialVertex> partition : partitions) {
			Map<SampledSocialVertex, Color> sourceColors = new HashMap<SampledSocialVertex, Color>();
			Map<SampledSocialVertex, SampledSocialVertex> sourceMapping = new HashMap<SampledSocialVertex, SampledSocialVertex>();
			for(SpatialVertex vertex : partition) {
//				if(((SampledSocialVertex)vertex).getSources().size() > 1) {
//					if(((SampledSocialVertex)vertex).isSampled())
//						colors.put(vertex, Color.WHITE);
//					else
//						colors.put(vertex, Color.BLACK);
//				} else {
					SampledSocialVertex source = getSource((SampledSocialVertex) vertex);
					sourceMapping.put((SampledSocialVertex) vertex, source);
					Color c = sourceColors.get(source);
					if(c == null) {
						c = ColorUtils.getGRBColor(sourceColors.size()/5.0 + 0.1);
						sourceColors.put(source, c);
					}
					colors.put(vertex, c);
//				}
			}
			
			for(SpatialVertex vertex : partition) {
				if(((SampledSocialVertex)vertex).getSources().size() > 1) {
					SampledSocialVertex s1 = ((SampledSocialVertex)vertex).getSources().get(0);
					SampledSocialVertex s2 = ((SampledSocialVertex)vertex).getSources().get(1);
					if(sourceMapping.get(s1) != sourceMapping.get(s2)) {
						colors.put(vertex, Color.WHITE);
						System.err.println(((SampledSocialVertex)vertex).getPerson().getId().toString());
					}
				}
			}
			
			StringBuilder builder = new StringBuilder();
			for(SampledSocialVertex vertex : sourceColors.keySet()) {
				builder.append(vertex.getPerson().getId().toString());
				builder.append(" ");
			}
			
			System.out.println(String.format("Component n=%1$s has seeds %2$s", partition.size(), builder.toString()));
		}
		return partitions;
	}

	private SampledSocialVertex getSource(SampledSocialVertex vertex) {
		if(vertex.getSources().isEmpty())
			return vertex;
		else
			return getSource(vertex.getSources().get(0));
	}
}
