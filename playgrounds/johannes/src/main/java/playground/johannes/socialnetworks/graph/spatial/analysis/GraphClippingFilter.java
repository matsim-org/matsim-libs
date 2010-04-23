/* *********************************************************************** *
 * project: org.matsim.*
 * GraphClippingFilter.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.util.HashSet;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class GraphClippingFilter implements GraphFilter<SpatialGraph> {

	private GraphBuilder<SpatialGraph, SpatialVertex, SpatialEdge> builder;

	private Geometry geometry;

	@SuppressWarnings("unchecked")
	public GraphClippingFilter(
			GraphBuilder<? extends SpatialGraph, ? extends SpatialVertex, ? extends SpatialEdge> builder,
			Geometry geometry) {
		this.builder = (GraphBuilder<SpatialGraph, SpatialVertex, SpatialEdge>) builder;
		this.geometry = geometry;
	}

	@Override
	public SpatialGraph apply(SpatialGraph graph) {
		Set<SpatialEdge> edges = new HashSet<SpatialEdge>();
		Set<SpatialVertex> vertices = new HashSet<SpatialVertex>();
		findElements(graph, vertices, edges);
		for (SpatialEdge edge : edges)
			builder.removeEdge(graph, edge);

		for (SpatialVertex vertex : vertices)
			builder.removeVertex(graph, vertex);

		return graph;

	}

	protected void findElements(SpatialGraph graph, Set<SpatialVertex> vertices, Set<SpatialEdge> edges) {
		try {
			CoordinateReferenceSystem sourceCRS = graph.getCoordinateReferenceSysten();
			CoordinateReferenceSystem targetCRS = CRSUtils.getCRS(geometry.getSRID());

			MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);

			for (SpatialVertex vertex : graph.getVertices()) {
				Point p = CRSUtils.transformPoint(vertex.getPoint(), transform);
				if (!geometry.contains(p)) {
					vertices.add(vertex);
					edges.addAll(vertex.getEdges());
				}

			}
		} catch (FactoryException e) {
			e.printStackTrace();
		}

	}
}
