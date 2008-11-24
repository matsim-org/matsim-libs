/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphMLReader.java
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

import org.matsim.gbl.Gbl;
import org.xml.sax.Attributes;

import playground.johannes.graph.AbstractSparseGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.io.AbstractGraphMLReader;

/**
 * @author illenberger
 *
 */
public class SampledGraphMLReader extends AbstractGraphMLReader {

	@Override
	protected AbstractSparseGraph newGraph(Attributes attrs) {
		return new SampledGraph();
	}

	@Override
	public SampledGraph readGraph(String file) {
		return (SampledGraph) super.readGraph(file);
	}

	@Override
	protected SparseEdge addEdge(SparseVertex v1, SparseVertex v2, Attributes attrs) {
		return ((SampledGraph)graph).addEdge((SampledVertex)v1, (SampledVertex)v2);
	}

	@Override
	protected SparseVertex addVertex(Attributes attrs) {
		return ((SampledGraph)graph).addVertex();
	}

	public static void main(String args[]) {
		Gbl.startMeasurement();
		AbstractSparseGraph g = new SampledGraphMLReader().readGraph("/Users/fearonni/vsp-work/socialnets/devel/snowball/data/networks/cond-mat-2005-gc.graphml");
		Gbl.printElapsedTime();
		System.out.println(g.toString());
		Gbl.printMemoryUsage();
	}
}
