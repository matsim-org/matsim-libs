/* *********************************************************************** *
 * project: org.matsim.*
 * F2FFrequency.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.social.analysis;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AbstractEdgeProperty;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialEdge;

/**
 * @author illenberger
 *
 */
public class F2FFrequency extends AbstractEdgeProperty {

	private static F2FFrequency instance;
	
	public static F2FFrequency getInstance() {
		if(instance == null)
			instance = new F2FFrequency();
		
		return instance;
	}
	
	@Override
	public TObjectDoubleHashMap<Edge> values(Set<? extends Edge> edges) {
		@SuppressWarnings("unchecked")
		Set<SocialEdge> socialEdges = (Set<SocialEdge>) edges;
		
		TObjectDoubleHashMap<Edge> values = new TObjectDoubleHashMap<Edge>();
		for(SocialEdge edge : socialEdges) {
			values.put(edge, edge.getFrequency());
		}
		
		return values;
	}

}
