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
package playground.johannes.socialnetworks.graph.spatial.io;

import org.matsim.contrib.sna.graph.io.AbstractGraphMLReader;
import org.matsim.core.utils.geometry.CoordImpl;
import org.xml.sax.Attributes;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraphBuilder;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;

/**
 * @author illenberger
 *
 */
public class SpatialGraphMLReader extends AbstractGraphMLReader<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> {

	public static final String COORD_X_TAG = "x";
	
	public static final String COORD_Y_TAG = "y";
	
	private SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder();
	
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
		double x = Double.parseDouble(attrs.getValue(COORD_X_TAG));
		double y = Double.parseDouble(attrs.getValue(COORD_Y_TAG));
		return builder.addVertex((SpatialSparseGraph)getGraph(), new CoordImpl(x, y));
	}

	@Override
	protected SpatialSparseGraph newGraph(Attributes attrs) {
		return new SpatialSparseGraph();
	}
}
