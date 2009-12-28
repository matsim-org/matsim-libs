/* *********************************************************************** *
 * project: org.matsim.*
 * PajekCommunityColorizer.java
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
package playground.johannes.socialnetworks.graph.io;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Vertex;


/**
 * @author illenberger
 *
 */
public class PajekCommunityColorizer <V extends Vertex, E extends Edge> extends PajekColorizer<V, E> {

	private TObjectDoubleHashMap<V> values = new TObjectDoubleHashMap<V>();
	
	public PajekCommunityColorizer(Set<Set<V>> clusters) {
		double value = 1;
		for(Set<V> cluster : clusters) {
			for(V vertex : cluster) {
				values.put(vertex, value);
			}
			value -= 0.05;
			value = Math.max(value, 0);
		}
	}
	
	@Override
	public String getEdgeColor(E e) {
		return getColor(-1);
	}

	@Override
	public String getVertexFillColor(V v) {
		return getColor(values.get(v));
	}

}
