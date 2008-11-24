/* *********************************************************************** *
 * project: org.matsim.*
 * PlainGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * @author illenberger
 *
 */
public class PlainGraph extends AbstractSparseGraph {

	public SparseVertex addVertex() {
		SparseVertex v = new SparseVertex();
		if(insertVertex(v))
			return v;
		else
			return null;
	}
	
	public SparseEdge addEdge(SparseVertex v1, SparseVertex v2) {
		SparseEdge e = new SparseEdge(v1, v2);
		if(insertEdge(e, v1, v2))
			return e;
		else
			return null;
	}
	
}
