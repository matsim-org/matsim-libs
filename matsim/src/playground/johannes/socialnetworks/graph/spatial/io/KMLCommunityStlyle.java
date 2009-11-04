/* *********************************************************************** *
 * project: org.matsim.*
 * KMLCommunityStlyle.java
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
package playground.johannes.socialnetworks.graph.spatial.io;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;

import net.opengis.kml._2.LinkType;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;

/**
 * @author illenberger
 *
 */
public class KMLCommunityStlyle <G extends Graph, V extends SpatialSparseVertex> extends KMLVertexColorStyle<G, V> {

	private Set<Set<V>> clusters;
	
	private TObjectDoubleHashMap<V> values = new TObjectDoubleHashMap<V>();
	
	private static final String VERTEX_STYLE_PREFIX = "vertex.style.";
	
	public KMLCommunityStlyle(LinkType vertexIconLink, Set<Set<V>> clusters) {
		super(vertexIconLink);
		this.clusters = clusters;
	}

	@Override
	protected double getValue(V vertex) {
		return values.get(vertex);
	}

	@Override
	protected TDoubleObjectHashMap<String> getValues(G graph) {
		TDoubleObjectHashMap<String> styles = new TDoubleObjectHashMap<String>();
		
		double value = 20;
		for(Set<V> cluster : clusters) {
			for(V vertex : cluster) {
				values.put(vertex, value);
			}
			value--;
			value = Math.max(value, 0);
			
			styles.put(value, VERTEX_STYLE_PREFIX+value);
		}
		
		return styles;
	}

}
