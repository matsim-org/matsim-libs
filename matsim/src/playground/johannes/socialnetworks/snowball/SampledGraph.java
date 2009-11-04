/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.snowball;

import java.util.Set;

import org.matsim.contrib.sna.graph.SparseGraph;


/**
 * @author illenberger
 *
 */
public class SampledGraph extends SparseGraph {

//	public SampledEdge addEdge(SampledVertex v1, SampledVertex v2) {
//		SampledEdge e = new SampledEdge(v1, v2);
//		if(insertEdge(e))
//			return e;
//		else
//			return null;
//	}
//
//	public SampledVertex addVertex() {
//		SampledVertex v = new SampledVertex();
//		if(insertVertex(v))
//			return v;
//		else
//			return null;
//	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SampledEdge> getEdges() {
		return (Set<? extends SampledEdge>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SampledVertex> getVertices() {
		return (Set<? extends SampledVertex>) super.getVertices();
	}
	
	public void reset() {
		for(SampledVertex v : getVertices())
			v.reset();
		for(SampledEdge e : getEdges())
			e.reset();
	}

}
