/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphProjectionTest.java
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

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialEdge;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraphBuilder;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseVertex;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;

import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraphProjection;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraphProjectionBuilder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author jillenberger
 *
 */
public class SampledSpatialGraphProjectionTest extends TestCase {

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
		geometry.setSRID(21781);
		/*
		 * Create a spatial graph with vertices inside and outside the geometry.
		 */
		SampledSpatialGraphBuilder builder = new SampledSpatialGraphBuilder(CRSUtils.getCRS(21781)); //TODO use CH1903LV03 for now.
		SampledSpatialSparseGraph graph = builder.createGraph();
		
		SampledSpatialSparseVertex v1 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(0, 0)));
		SampledSpatialSparseVertex v2 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(0, 10)));
		SampledSpatialSparseVertex v3 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(0, 20)));
		SampledSpatialSparseVertex v4 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(10, 20)));
		SampledSpatialSparseVertex v5 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(10, 10)));
		SampledSpatialSparseVertex v6 = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(10, 0)));
		
		builder.addEdge(graph, v1, v2);
		builder.addEdge(graph, v2, v3);
		builder.addEdge(graph, v3, v4);
		builder.addEdge(graph, v4, v5);
		builder.addEdge(graph, v5, v6);
		builder.addEdge(graph, v6, v1);
		builder.addEdge(graph, v2, v5);
		/*
		 * apply snowball attributes
		 */
		v1.detect(0);
		v1.sample(0);
		
		v2.detect(0);
		v6.detect(0);
		v2.sample(1);
		v6.sample(1);
		
		v3.detect(1);
		v5.detect(1);
		v5.sample(2);
		v3.sample(2);
		
		v4.detect(2);
		/*
		 * Create the projection.
		 */
		SampledSpatialGraphProjectionBuilder<SampledSpatialGraph, SampledSpatialVertex, ?> projBuilder = new SampledSpatialGraphProjectionBuilder<SampledSpatialGraph, SampledSpatialVertex, SampledSpatialEdge>();
		SampledSpatialGraphProjection<?, SampledSpatialVertex, ?> projection = projBuilder.decorate(graph, geometry);
		/*
		 * test snowball attributes
		 */
		assertEquals(projection.getVertex(v1).getIterationDetected(), 0);
		assertEquals(projection.getVertex(v1).getIterationSampled(), 0);
		
		assertEquals(projection.getVertex(v2).getIterationDetected(), 0);
		assertEquals(projection.getVertex(v2).getIterationSampled(), 1);
		
		assertEquals(projection.getVertex(v5).getIterationDetected(), 1);
		assertEquals(projection.getVertex(v5).getIterationSampled(), 2);
		
		assertEquals(projection.getVertex(v6).getIterationDetected(), 0);
		assertEquals(projection.getVertex(v6).getIterationSampled(), 1);
		
		assertNull(projection.getVertex(v3));
		assertNull(projection.getVertex(v4));
	}
}
