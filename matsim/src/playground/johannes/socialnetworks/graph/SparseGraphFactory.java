/* *********************************************************************** *
 * project: org.matsim.*
 * PlainGraphFactory.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph;


/**
 * @author illenberger
 *
 */
public class SparseGraphFactory implements GraphFactory<SparseGraph, SparseVertex, SparseEdge> {

	public SparseEdge addEdge(SparseGraph g, SparseVertex v1, SparseVertex v2) {
		SparseEdge e = new SparseEdge(v1, v2);
		if (g.insertEdge(e, v1, v2))
			return e;
		else
			return null;
	}

	public SparseVertex addVertex(SparseGraph g) {
		SparseVertex v = new SparseVertex();
		if (g.insertVertex(v))
			return v;
		else
			return null;
	}

	public SparseGraph createGraph() {
		return new SparseGraph();
	}

}
