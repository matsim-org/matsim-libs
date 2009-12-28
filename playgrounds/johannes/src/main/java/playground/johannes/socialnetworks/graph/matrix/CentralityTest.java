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
package playground.johannes.socialnetworks.graph.matrix;

import java.io.IOException;

import junit.framework.TestCase;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class CentralityTest extends TestCase {
	
	public void test() throws IOException {
		AdjacencyMatrix y = new AdjacencyMatrix();
		
		for(int i = 0; i < 7; i++)
			y.addVertex();
		
		y.addEdge(0, 2);
		y.addEdge(1, 2);
		y.addEdge(2, 3);
		y.addEdge(3, 4);
		y.addEdge(3, 5);
		
		Centrality c = new Centrality();
		c.run(y);
		
		assertEquals(0, c.getVertexBetweenness()[0]);
		assertEquals(0, c.getVertexBetweenness()[1]);
		assertEquals(14, c.getVertexBetweenness()[2]);
		assertEquals(14, c.getVertexBetweenness()[3]);
		assertEquals(0, c.getVertexBetweenness()[4]);
		assertEquals(0, c.getVertexBetweenness()[5]);
		
		assertEquals(2.2, c.getVertexCloseness()[0]);
		assertEquals(2.2, c.getVertexCloseness()[1]);
		assertEquals(1.4, c.getVertexCloseness()[2]);
		assertEquals(1.4, c.getVertexCloseness()[3]);
		assertEquals(2.2, c.getVertexCloseness()[4]);
		assertEquals(2.2, c.getVertexCloseness()[5]);
		assertEquals(true, Double.isInfinite(c.getVertexCloseness()[6]));
		
		assertEquals(10, c.getEdgeBetweenness()[0].get(2));
		assertEquals(10, c.getEdgeBetweenness()[1].get(2));
		assertEquals(18, c.getEdgeBetweenness()[2].get(3));
		assertEquals(10, c.getEdgeBetweenness()[3].get(4));
		assertEquals(10, c.getEdgeBetweenness()[3].get(5));
	}
}
