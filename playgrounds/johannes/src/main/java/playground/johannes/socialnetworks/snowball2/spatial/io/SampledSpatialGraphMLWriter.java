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
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.snowball2.SampledGraph;
import playground.johannes.socialnetworks.snowball2.SampledVertex;

/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphMLWriter extends SpatialGraphMLWriter {

	public static final String DETECTED_TAG = "detected";
	
	public static final String SAMPLED_TAG = "sampled";
	
	@Override
	public void write(Graph graph, String filename) throws IOException {
		if(graph instanceof SpatialSparseGraph && graph instanceof SampledGraph)
			super.write(graph, filename);
		else
			throw new IllegalArgumentException("Graph has to be a SpatialGraph and SampledGraph!");
	}

	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attributes = super.getVertexAttributes(v);
		
		int detected = ((SampledVertex)v).getIterationDetected();
		int sampled = ((SampledVertex)v).getIterationSampled();
		
		if(detected > -1)
			attributes.add(new Tuple<String, String>(DETECTED_TAG, String.valueOf(detected)));
		if(sampled > -1)
			attributes.add(new Tuple<String, String>(SAMPLED_TAG, String.valueOf(sampled)));
		
		return attributes;
	}

}
