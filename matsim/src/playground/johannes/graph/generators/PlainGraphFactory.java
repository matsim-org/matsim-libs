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
package playground.johannes.graph.generators;

import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;

/**
 * @author illenberger
 *
 */
public class PlainGraphFactory implements GraphFactory<PlainGraph, SparseVertex, SparseEdge> {

	public SparseEdge addEdge(PlainGraph g, SparseVertex v1, SparseVertex v2) {
		return g.addEdge(v1, v2);
	}

	public SparseVertex addVertex(PlainGraph g) {
		return g.addVertex();
	}

	public PlainGraph createGraph() {
		return new PlainGraph();
	}

}
