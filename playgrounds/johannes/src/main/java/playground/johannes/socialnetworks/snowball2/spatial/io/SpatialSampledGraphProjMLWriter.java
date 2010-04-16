/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialSampledGraphProjMLWriter.java
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
package playground.johannes.socialnetworks.snowball2.spatial.io;

import java.util.List;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphML;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLWriter;

/**
 * @author illenberger
 *
 */
public class SpatialSampledGraphProjMLWriter extends SampledGraphProjMLWriter {

	public SpatialSampledGraphProjMLWriter(GraphMLWriter delegateWriter) {
		super(delegateWriter);
	}

	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = super.getVertexAttributes(v);
		SpatialGraphML.addPointData((SpatialVertex) v, attrs);
		return attrs;
	}

}
