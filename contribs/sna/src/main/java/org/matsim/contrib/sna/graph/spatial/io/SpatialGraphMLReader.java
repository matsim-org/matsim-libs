/* *********************************************************************** *
 * project: org.matsim.*
 * SNGraphMLReader.java
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

/**
 * 
 */
package org.matsim.contrib.sna.graph.spatial.io;

import org.matsim.contrib.sna.graph.io.AbstractGraphMLReader;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;


/**
 * @author illenberger
 *
 */
public class SpatialGraphMLReader extends AbstractGraphMLReader<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> {

	private SpatialSparseGraphBuilder builder;
	
	@Override
	public SpatialSparseGraph readGraph(String file) {
		return (SpatialSparseGraph) super.readGraph(file);
	}

	@Override
	protected SpatialSparseEdge addEdge(SpatialSparseVertex v1, SpatialSparseVertex v2,
			Attributes attrs) {
		return builder.addEdge(getGraph(), v1, v2);
	}

	@Override
	protected SpatialSparseVertex addVertex(Attributes attrs) {
		return builder.addVertex((SpatialSparseGraph)getGraph(), SpatialGraphML.newPoint(attrs));
	}

	@Override
	protected SpatialSparseGraph newGraph(Attributes attrs) {
		CoordinateReferenceSystem crs = SpatialGraphML.newCRS(attrs);
		builder = new SpatialSparseGraphBuilder(crs);
		return builder.createGraph();
	}
}
