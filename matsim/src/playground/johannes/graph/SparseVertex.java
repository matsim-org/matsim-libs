/* *********************************************************************** *
 * project: org.matsim.*
 * SparseVertex.java
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
package playground.johannes.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class SparseVertex implements Vertex {

	private ArrayList<SparseEdge> edges;
	
	private ArrayList<SparseVertex> neighbours;
	
	protected SparseVertex() {
		edges = new ArrayList<SparseEdge>();
		neighbours = new ArrayList<SparseVertex>();
	}
	
	public List<? extends SparseEdge> getEdges() {
		return edges;
	}

	public List<? extends SparseVertex> getNeighbours() {
		return neighbours;
	}

	void addEdge(SparseEdge e) {
		edges.add(e);
		neighbours.add(e.getOpposite(this));
	}
	
	void optimize() {
		edges.trimToSize();
		neighbours.trimToSize();
	}
}
