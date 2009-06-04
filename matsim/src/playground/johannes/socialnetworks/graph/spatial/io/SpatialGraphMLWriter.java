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

import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.Graph;
import playground.johannes.socialnetworks.graph.Vertex;
import playground.johannes.socialnetworks.graph.io.GraphMLWriter;
import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;

/**
 * @author illenberger
 *
 */
public class SpatialGraphMLWriter extends GraphMLWriter {

	@Override
	public void write(Graph graph, String filename) throws IOException {
		if(graph instanceof SpatialGraph)
			super.write(graph, filename);
		else
			throw new ClassCastException("Graph must be of type SpatialGraph.");
	}

	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = super.getVertexAttributes(v);
		
		Ego<?> e = (Ego<?>)v;
		attrs.add(new Tuple<String, String>(SpatialGraphMLReader.COORD_X_TAG, String.valueOf(e.getCoordinate().getX())));
		attrs.add(new Tuple<String, String>(SpatialGraphMLReader.COORD_Y_TAG, String.valueOf(e.getCoordinate().getY())));
		
		return attrs;
	}
}
