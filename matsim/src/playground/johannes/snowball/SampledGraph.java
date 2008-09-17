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
package playground.johannes.snowball;

import java.util.Set;

import playground.johannes.graph.SparseGraph;
import playground.johannes.graph.SparseVertex;

/**
 * @author illenberger
 *
 */
public class SampledGraph extends SparseGraph {

	@Override
	public SampledEdge addEdge(SparseVertex v1, SparseVertex v2) {
		return (SampledEdge) super.addEdge(v1, v2);
	}

	@Override
	public SampledVertex addVertex() {
		return (SampledVertex) super.addVertex();
	}

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

	@Override
	protected SampledEdge newEdge(SparseVertex v1, SparseVertex v2) {
		if(v1 instanceof SampledVertex && v2 instanceof SampledVertex)
			return new SampledEdge((SampledVertex)v1, (SampledVertex)v2);
		else
			throw new IllegalArgumentException("Vertex must be instance of SampledVertex.");
	}

	@Override
	protected SampledVertex newVertex() {
		return new SampledVertex();
	}
	
	public void reset() {
		for(SampledVertex v : getVertices())
			v.reset();
		for(SampledEdge e : getEdges())
			e.reset();
	}

}
