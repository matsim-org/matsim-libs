/* *********************************************************************** *
 * project: org.matsim.*
 * SNGraphMLWriter.java
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

import java.io.IOException;
import java.util.List;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.io.GraphMLWriter;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;
import playground.johannes.socialnetworks.spatial.CRSUtils;

/**
 * @author illenberger
 *
 */
public class SpatialGraphMLWriter extends GraphMLWriter {

	@Override
	public void write(Graph graph, String filename) throws IOException {
		if(graph instanceof SpatialSparseGraph)
			super.write(graph, filename);
		else
			throw new ClassCastException("Graph must be of type SpatialGraph.");
	}

	@Override
	protected SpatialGraph getGraph() {
		return (SpatialGraph) super.getGraph();
	}

	@Override
	protected List<Tuple<String, String>> getGraphAttributes() {
		List<Tuple<String, String>> attrs = super.getGraphAttributes();
		int srid = CRSUtils.getSRID(getGraph().getCoordinateReferenceSysten());
		attrs.add(new Tuple<String, String>(SpatialGraphML.SRID_ATTR, String.valueOf(srid)));
		
		return attrs;
	}

	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = super.getVertexAttributes(v);
		
		attrs.add(new Tuple<String, String>(SpatialGraphML.COORD_X_TAG, String.valueOf(((SpatialSparseVertex)v).getCoordinate().getX())));
		attrs.add(new Tuple<String, String>(SpatialGraphML.COORD_Y_TAG, String.valueOf(((SpatialSparseVertex)v).getCoordinate().getY())));
		
		return attrs;
	}
}
