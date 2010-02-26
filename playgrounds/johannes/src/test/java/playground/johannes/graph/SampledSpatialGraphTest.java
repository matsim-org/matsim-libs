/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphTest.java
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
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraphBuilder;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseGraph;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author jillenberger
 *
 */
public class SampledSpatialGraphTest extends TestCase {

	public void test() {
		SampledSpatialGraphBuilder builder = new SampledSpatialGraphBuilder(CRSUtils.getCRS(21781));
		SampledSpatialSparseGraph graph = builder.createGraph();
		
		GeometryFactory factory = new GeometryFactory();
		SampledSpatialSparseVertex v1 = builder.addVertex(graph, factory.createPoint(new Coordinate(0, 0)));
		SampledSpatialSparseVertex v2 = builder.addVertex(graph, factory.createPoint(new Coordinate(10, 0)));
		SampledSpatialSparseVertex v3 = builder.addVertex(graph, factory.createPoint(new Coordinate(10, 10)));
		SampledSpatialSparseVertex v4 = builder.addVertex(graph, factory.createPoint(new Coordinate(0, 10)));
		
		builder.addEdge(graph, v1, v2);
		builder.addEdge(graph, v2, v3);
		builder.addEdge(graph, v3, v4);
		builder.addEdge(graph, v4, v1);
		
		v1.detect(0);
		v1.sample(0);
		
		v2.detect(0);
		v4.detect(0);
		
		v2.sample(1);
		v4.sample(1);
		v3.detect(1);
		
		assertEquals(0, v1.getIterationDetected());
		assertEquals(0, v1.getIterationSampled());
		assertTrue(v1.isDetected());
		assertTrue(v1.isSampled());
		
		assertEquals(0, v2.getIterationDetected());
		assertEquals(1, v2.getIterationSampled());
		assertTrue(v2.isDetected());
		assertTrue(v2.isSampled());
		
		assertEquals(0, v4.getIterationDetected());
		assertEquals(1, v4.getIterationSampled());
		assertTrue(v4.isDetected());
		assertTrue(v4.isSampled());
		
		assertEquals(1, v3.getIterationDetected());
		assertEquals(-1, v3.getIterationSampled());
		assertTrue(v3.isDetected());
		assertTrue(!v3.isSampled());
		
		for(SampledVertex v : v1.getNeighbours()) {
			assertEquals(0, v.getIterationDetected());
			assertEquals(1, v.getIterationSampled());
		}
		
		assertEquals(10.0, graph.getEdge(v1, v2).length(), 0.01);
		assertEquals(10.0, graph.getEdge(v2, v3).length(), 0.01);
		assertEquals(10.0, graph.getEdge(v3, v4).length(), 0.01);
		assertEquals(10.0, graph.getEdge(v4, v1).length(), 0.01);
	}
}
