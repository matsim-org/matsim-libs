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

import org.matsim.core.utils.geometry.CoordImpl;
import org.xml.sax.Attributes;

import playground.johannes.socialnetworks.graph.SparseVertex;
import playground.johannes.socialnetworks.graph.io.AbstractGraphMLReader;
import playground.johannes.socialnetworks.graph.spatial.SpatialEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphFactory;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;

/**
 * @author illenberger
 *
 */
public class SpatialGraphMLReader extends AbstractGraphMLReader {

	public static final String COORD_X_TAG = "x";
	
	public static final String COORD_Y_TAG = "y";
	
	private SpatialGraphFactory factory = new SpatialGraphFactory();
	
	@Override
	public SpatialGraph readGraph(String file) {
		return (SpatialGraph) super.readGraph(file);
	}

	@Override
	protected SpatialEdge addEdge(SparseVertex v1, SparseVertex v2,
			Attributes attrs) {
		return factory.addEdge((SpatialGraph)graph, (SpatialVertex)v1, (SpatialVertex)v2);
	}

	@Override
	protected SpatialVertex addVertex(Attributes attrs) {
		double x = Double.parseDouble(attrs.getValue(COORD_X_TAG));
		double y = Double.parseDouble(attrs.getValue(COORD_Y_TAG));
		return factory.addVertex((SpatialGraph)graph, new CoordImpl(x, y));
	}

	@Override
	protected SpatialGraph newGraph(Attributes attrs) {
		return new SpatialGraph();
	}
}
