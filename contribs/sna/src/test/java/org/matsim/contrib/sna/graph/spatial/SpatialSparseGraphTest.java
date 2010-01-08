/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialSparseGraphTest.java
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
package org.matsim.contrib.sna.graph.spatial;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import junit.framework.TestCase;

/**
 * @author illenberger
 *
 */
public class SpatialSparseGraphTest extends TestCase {

	private static final double EPSILON = 0.01;
	
	public void test() {
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = builder.createGraph();
		
		GeometryFactory geometryFactory = new GeometryFactory();
		
		SpatialSparseVertex v1 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(0, 0)));
		SpatialSparseVertex v2 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(0, 10)));
		SpatialSparseVertex v3 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(10, 10)));
		SpatialSparseVertex v4 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(10, 0)));
		
		builder.addEdge(graph, v1, v2);
		builder.addEdge(graph, v2, v3);
		builder.addEdge(graph, v3, v4);
		builder.addEdge(graph, v4, v1);
		builder.addEdge(graph, v1, v3);
		builder.addEdge(graph, v4, v2);
		
		assertEquals(10.0, graph.getEdge(v1, v2).length(), EPSILON);
		assertEquals(10.0, graph.getEdge(v2, v3).length(), EPSILON);
		assertEquals(10.0, graph.getEdge(v3, v4).length(), EPSILON);
		assertEquals(10.0, graph.getEdge(v4, v1).length(), EPSILON);
		assertEquals(14.1421, graph.getEdge(v1, v3).length(), EPSILON);
		assertEquals(14.1421, graph.getEdge(v2, v4).length(), EPSILON);
	}
}
