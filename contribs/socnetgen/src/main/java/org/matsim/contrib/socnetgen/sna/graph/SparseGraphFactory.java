/* *********************************************************************** *
 * project: org.matsim.*
 * SparseGraphFactory2.java
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
package org.matsim.contrib.socnetgen.sna.graph;

/**
 * Implementation of GraphFactory to creates instances of SparseGraph,
 * SparseVertex and SparseEdge.
 * 
 * @author illenberger
 * 
 */
public class SparseGraphFactory implements
		GraphFactory<SparseGraph, SparseVertex, SparseEdge> {

	/**
	 * Creates and returns an empty SparseGraph.
	 * 
	 * @return an empty SparseGraph.
	 */
	@Override
	public SparseGraph createGraph() {
		return new SparseGraph();
	}

	/**
	 * Creates and returns an isolated SparseVertex.
	 * 
	 * @returns an isolated SparseVertex
	 */
	@Override
	public SparseVertex createVertex() {
		return new SparseVertex();
	}

	/**
	 * Creates and returns an orphaned SparseEdge.
	 * 
	 * @return an orphaned SparseEdge.
	 */
	@Override
	public SparseEdge createEdge() {
		return new SparseEdge();
	}

	@Override
	public SparseGraph copyGraph(SparseGraph graph) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public SparseVertex copyVertex(SparseVertex vertex) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public SparseEdge copyEdge(SparseEdge edge) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

}
