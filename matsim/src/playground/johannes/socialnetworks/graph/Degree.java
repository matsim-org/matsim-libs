/* *********************************************************************** *
 * project: org.matsim.*
 * Degree.java
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
package playground.johannes.socialnetworks.graph;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;

import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class Degree<V extends Vertex> {

	public Distribution distribution(Collection<V> vertices) {
		Distribution distribution = new Distribution();
		for(V v : vertices)
			distribution.add(v.getEdges().size());
		
		return distribution;
	}
	
	public TObjectDoubleHashMap<V> values(Collection<V> vertices) {
		TObjectDoubleHashMap<V> values = new TObjectDoubleHashMap<V>();
		for(V v : vertices)
			values.put(v, v.getEdges().size());
		
		return values;
	}
}
