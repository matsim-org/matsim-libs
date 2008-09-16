/* *********************************************************************** *
 * project: org.matsim.*
 * SparseGraph.java
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
package playground.johannes.snowball2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class SparseGraph {

	private List<SparseVertex> vertices;
	
	private List<SparseEdge> edges;
	
	public SparseGraph(int numVertex, int numEdge) {
		vertices = new ArrayList<SparseVertex>(numVertex);
		edges = new ArrayList<SparseEdge>(numEdge);
	}
	
	protected void addVertex(SparseVertex v) {
		vertices.add(v);
	}
	
	protected void addEdge(SparseVertex v1, SparseVertex v2) {
		SparseEdge e = newEdge(v1,  v2);
		v1.addEdge(e);
		v2.addEdge(e);
		edges.add(e);
	}
	
	protected SparseVertex newVertex() {
		return new SparseVertex();
	}
	
	protected SparseEdge newEdge(SparseVertex v1, SparseVertex v2) {
		return new SparseEdge(v1, v2);
	}
	
	public Collection<SparseVertex> getVertices() {
		return vertices;
	}
	
	public Collection<SparseEdge> getEdges() {
		return edges;
	}
}
