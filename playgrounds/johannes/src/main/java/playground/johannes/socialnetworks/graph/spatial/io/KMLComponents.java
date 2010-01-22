/* *********************************************************************** *
 * project: org.matsim.*
 * KMLComponents.java
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
package playground.johannes.socialnetworks.graph.spatial.io;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import net.opengis.kml._2.FolderType;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.ColorUtils;
import org.matsim.contrib.sna.graph.spatial.io.Colorizable;
import org.matsim.contrib.sna.graph.spatial.io.KMLPartitions;

import playground.johannes.socialnetworks.graph.Partitions;

/**
 * @author illenberger
 *
 */
public class KMLComponents implements KMLPartitions, Colorizable<Vertex> {

	private Map<SpatialVertex, Color> colors;
	
	@Override
	public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition) {
		kmlFolder.setName("n="+partition.size());
	}

	@Override
	public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
		SortedSet<Set<SpatialVertex>> partitions = Partitions.disconnectedComponents(graph);
		
		colors = new HashMap<SpatialVertex, Color>();
		int size = partitions.size();
		int idx = 1;
		for(Set<SpatialVertex> partition : partitions) {
			Color c = ColorUtils.getGRBColor(idx/(double)size);
			for(SpatialVertex v : partition) {
				colors.put(v, c);
			}
			idx++;
		}
		return new ArrayList<Set<? extends SpatialVertex>>(partitions);
	}

	@Override
	public Color getColor(Vertex object) {
		return colors.get(object);
	}

}
