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
package playground.johannes.socialnetworks.snowball;

import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.io.AbstractGraphMLReader;
import org.matsim.core.gbl.Gbl;
import org.xml.sax.Attributes;


/**
 * @author illenberger
 *
 */
public class SampledGraphMLReader extends AbstractGraphMLReader<SampledGraph, SampledVertex, SampledEdge> {

	private SampledGraphBuilder builder = new SampledGraphBuilder();
	
	@Override
	protected SampledGraph newGraph(Attributes attrs) {
		return new SampledGraph();
	}

	@Override
	public SampledGraph readGraph(String file) {
		return (SampledGraph) super.readGraph(file);
	}

	@Override
	protected SampledEdge addEdge(SampledVertex v1, SampledVertex v2, Attributes attrs) {
		return builder.addEdge(((SampledGraph)getGraph()), (SampledVertex)v1, (SampledVertex)v2);
	}

	@Override
	protected SampledVertex addVertex(Attributes attrs) {
		return builder.addVertex(((SampledGraph)getGraph()));
	}

	public static void main(String args[]) {
		Gbl.startMeasurement();
		SparseGraph g = new SampledGraphMLReader().readGraph("/Users/fearonni/vsp-work/socialnets/devel/snowball/data/networks/cond-mat-2005-gc.graphml");
		Gbl.printElapsedTime();
		System.out.println(g.toString());
		Gbl.printMemoryUsage();
	}
}
