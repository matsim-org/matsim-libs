/* *********************************************************************** *
 * project: org.matsim.*
 * SparseEdge.java
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

import org.matsim.utils.collections.Tuple;

/**
 * @author illenberger
 *
 */
public class SparseEdge implements Edge {

	private Tuple<SparseVertex, SparseVertex> vertices;
	
	public SparseEdge(SparseVertex v1, SparseVertex v2) {
		vertices = new Tuple<SparseVertex, SparseVertex>(v1, v2);
	}
	
	public SparseVertex getOpposite(Vertex v) {
		if(vertices.getFirst().equals(v))
			return vertices.getSecond();
		else if(vertices.getSecond().equals(v))
			return vertices.getFirst();
		else
			return null;
	}

	public Tuple<? extends SparseVertex, ? extends SparseVertex> getVertices() {
		return vertices;
	}

}
