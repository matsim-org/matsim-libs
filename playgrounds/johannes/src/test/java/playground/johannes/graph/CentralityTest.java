/* *********************************************************************** *
 * project: org.matsim.*
 * CentralityTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.graph;

import junit.framework.TestCase;

import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;

import playground.johannes.socialnetworks.graph.analysis.Centrality;

/**
 * @author illenberger
 *
 */
public class CentralityTest extends TestCase {

	public void test() {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		SparseGraph graph = reader.readGraph("/Users/jillenberger/Work/work/socialnets/snowball/data/networks/cond-mat-2005-gc.graphml");
		
//		System.out.println("Old version...");
//		long time = System.currentTimeMillis();
//		GraphDistance dist = GraphStatistics.centrality(graph);
//		System.out.println("Took " + (System.currentTimeMillis() - time));
//		
//		System.out.println("Closeness = " + dist.getGraphCloseness());
//		System.out.println("Betweenness =" + dist.getGraphBetweenness());
//		System.out.println("Diameter = " + dist.getDiameter());
//		System.out.println("Radius = " + dist.getRadius());
//		
		System.out.println("New version...");
		Centrality centrality = new Centrality();
		long time = System.currentTimeMillis();
		centrality.init(graph);
		System.out.println("Took " + (System.currentTimeMillis() - time));
		
		System.out.println("Closeness = " + centrality.closenessDistribution().getMean());
		System.out.println("Betweenness =" + centrality.vertexBetweennessDistribution().getMean());
		System.out.println("Diameter = " + centrality.diameter());
		System.out.println("Radius = " + centrality.radius());
	}
}
