/* *********************************************************************** *
 * project: org.matsim.*
 * Test.java
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

package playground.johannes.mcmc;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseVertex;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PlainGraph g = new PlainGraph();
		
		SparseVertex v1 = g.addVertex();
		SparseVertex v2 = g.addVertex();
		SparseVertex v3 = g.addVertex();
		SparseVertex v4 = g.addVertex();
		SparseVertex v5 = g.addVertex();
		
		g.addEdge(v1, v2);
		g.addEdge(v1, v4);
		g.addEdge(v2, v3);
		g.addEdge(v3, v4);
		g.addEdge(v3, v5);
		g.addEdge(v5, v4);
		
		System.out.println("<k> = " + GraphStatistics.getDegreeStatistics(g).getMean());
		
		g.addEdge(v1, v3);
		g.addEdge(v2, v4);
		
		System.out.println("<k> = " + GraphStatistics.getDegreeStatistics(g).getMean());
		
		g.removeEdge(g.getEdge(v1, v2));
		g.removeEdge(g.getEdge(v1, v4));
		g.removeEdge(g.getEdge(v3, v4));
		g.removeEdge(g.getEdge(v1, v3));
		
		System.out.println("<k> = " + GraphStatistics.getDegreeStatistics(g).getMean());
	}

}
