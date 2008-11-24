/* *********************************************************************** *
 * project: org.matsim.*
 * PlainGraphMLReader.java
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
package playground.johannes.graph.io;

import org.xml.sax.Attributes;

import playground.johannes.graph.AbstractSparseGraph;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;

/**
 * @author illenberger
 *
 */
public class PlainGraphMLReader extends AbstractGraphMLReader {

	@Override
	protected SparseEdge addEdge(SparseVertex v1, SparseVertex v2,
			Attributes attrs) {
		return ((PlainGraph)graph).addEdge(v1, v2);
	}

	@Override
	protected SparseVertex addVertex(Attributes attrs) {
		return ((PlainGraph)graph).addVertex();
	}

	@Override
	protected AbstractSparseGraph newGraph(Attributes attrs) {
		return new PlainGraph();
	}

}
