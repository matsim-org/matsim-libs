/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphProjectionTest.java
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

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphProjection;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphProjectionBuilder;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraphBuilder;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertexDecorator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author illenberger
 *
 */
public class SpatialGraphProjectionTest extends TestCase {

	public void test() {
		/*
		 * Create a geometry (rectangle).
		 */
		GeometryFactory geometryFactory = new GeometryFactory();
		
		Coordinate[] coordinates = new Coordinate[5];
		coordinates[0] = new Coordinate(0, 0);
		coordinates[1] = new Coordinate(0, 10);
		coordinates[2] = new Coordinate(10, 10);
		coordinates[3] = new Coordinate(10, 0);
		coordinates[4] = coordinates[0];
		
		Geometry geometry = geometryFactory.createLinearRing(coordinates);
		/*
		 * Create a spatial graph with vertices inside and outside the geometry.
		 */
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder();
		SpatialSparseGraph graph = builder.createGraph();
		
		SpatialSparseVertex v1 = builder.addVertex(graph, new CoordImpl(0, 0));
		SpatialSparseVertex v2 = builder.addVertex(graph, new CoordImpl(0, 10));
		SpatialSparseVertex v3 = builder.addVertex(graph, new CoordImpl(0, 20));
		SpatialSparseVertex v4 = builder.addVertex(graph, new CoordImpl(10, 20));
		SpatialSparseVertex v5 = builder.addVertex(graph, new CoordImpl(10, 10));
		SpatialSparseVertex v6 = builder.addVertex(graph, new CoordImpl(10, 0));
		
		builder.addEdge(graph, v1, v2);
		builder.addEdge(graph, v2, v3);
		builder.addEdge(graph, v3, v4);
		builder.addEdge(graph, v4, v5);
		builder.addEdge(graph, v5, v6);
		builder.addEdge(graph, v6, v1);
		builder.addEdge(graph, v2, v5);
		/*
		 * Create the projection.
		 */
		SpatialGraphProjectionBuilder<SpatialGraph, SpatialVertex, ?> projBuilder = new SpatialGraphProjectionBuilder<SpatialGraph, SpatialVertex, SpatialEdge>();
		SpatialGraphProjection<?, SpatialVertex, ?> projection = projBuilder.decorate(graph, geometry);
		/*
		 * Test number of vertices and edges.
		 */
		assertEquals(4, projection.getVertices().size());
		assertEquals(4, projection.getEdges().size());
		/*
		 * Test id the correct vertices are in the projection.
		 */
		Set<SpatialVertex> inbounds = new HashSet<SpatialVertex>();
		inbounds.add(v1);
		inbounds.add(v2);
		inbounds.add(v5);
		inbounds.add(v6);
		
		for(SpatialVertex v : inbounds) {
			boolean found = false;
		
			for(SpatialVertexDecorator<?> vd : projection.getVertices()) {
				if(v == vd.getDelegate()) {
					found = true;
					break;
				}
			}
			
			assertTrue(found);
		}
		/*
		 * Test the coordinates.
		 */
		for(SpatialVertex v : inbounds) {
			assertEquals(v.getPoint(), projection.getVertex(v).getPoint());
		}
		/*
		 * Test if vertices outside of the geometry are not in the projection.
		 */
		assertNull(projection.getVertex(v3));
		assertNull(projection.getVertex(v4));
		/*
		 * Test degree.
		 */
		for(SpatialVertex v : inbounds) {
			assertEquals(2, projection.getVertex(v).getNeighbours().size());
		}
		
	}
}
