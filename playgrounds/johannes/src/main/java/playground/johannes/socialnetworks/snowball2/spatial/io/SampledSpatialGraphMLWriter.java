/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphMLWriter.java
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
package playground.johannes.socialnetworks.snowball2.spatial.io;

import java.io.IOException;
import java.util.List;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLWriter;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.io.SampledGraphML;
import org.matsim.core.utils.collections.Tuple;


/**
 * Extension to {@link SpatialGraphMLWriter} that includes writing snowball
 * sampling information.
 * 
 * @author illenberger
 * 
 */
public class SampledSpatialGraphMLWriter extends SpatialGraphMLWriter {

	/**
	 * @see {@link SpatialGraphMLWriter#write(Graph, String)}
	 */
	@Override
	public void write(Graph graph, String filename) throws IOException {
		if (graph instanceof SpatialSparseGraph
				&& graph instanceof SampledGraph)
			super.write(graph, filename);
		else
			throw new IllegalArgumentException(
					"Graph has to be a SpatialGraph and SampledGraph!");
	}

	/**
	 * Calls {@link SpatialGraphMLWriter#getVertexAttributes(Vertex)} and adds
	 * snowball sampling information to the attributes data.
	 * 
	 * @param a sampled spatial vertex.
	 */
	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attributes = super.getVertexAttributes(v);

		SampledGraphML.addSnowballAttributesData((SampledVertex) v, attributes);

		return attributes;
	}

}
