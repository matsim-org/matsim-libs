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
 * Class for creating {@link PlainGraph} objects out of GraphML files.
 * 
 * @author illenberger
 * 
 */
public class PlainGraphMLReader extends AbstractGraphMLReader {

	/**
	 * @see {@link AbstractGraphMLReader#readGraph(String)}
	 */
	@Override
	public PlainGraph readGraph(String file) {
		return (PlainGraph) super.readGraph(file);
	}

	/**
	 * Creates a new SparseEdge and inserts into the graph between <tt>v1</tt>
	 * and <tt>v2</tt>.
	 * 
	 * @param v1
	 *            one of the two vertices the edge is connected to.
	 * @param v2
	 *            one of the two vertices the edge is connected to.
	 * @param attrs
	 *            the edge's attributes.
	 * @return a new sparse edge.
	 * @see {@link AbstractGraphMLReader#addEdge(SparseVertex, SparseVertex, Attributes)}
	 *      .
	 */
	@Override
	protected SparseEdge addEdge(SparseVertex v1, SparseVertex v2,
			Attributes attrs) {
		return ((PlainGraph) graph).addEdge(v1, v2);
	}

	/**
	 * Creates a new SparseVertex and inserts into the graph.
	 * 
	 * @param attrs
	 *            the vertex's attributes.
	 * @return a new SparseVertex.
	 */
	@Override
	protected SparseVertex addVertex(Attributes attrs) {
		return ((PlainGraph) graph).addVertex();
	}

	/**
	 * Creates a new empty PlainGraph.
	 * 
	 * @param attrs
	 *            the graph's attributes (ignored).
	 */
	@Override
	protected AbstractSparseGraph newGraph(Attributes attrs) {
		return new PlainGraph();
	}

}
