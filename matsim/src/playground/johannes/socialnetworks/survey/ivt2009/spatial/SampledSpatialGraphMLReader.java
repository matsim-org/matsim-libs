/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphMLReader.java
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
package playground.johannes.socialnetworks.survey.ivt2009.spatial;

import org.matsim.core.utils.geometry.CoordImpl;
import org.xml.sax.Attributes;

import playground.johannes.socialnetworks.graph.SparseVertex;
import playground.johannes.socialnetworks.graph.io.AbstractGraphMLReader;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLReader;

/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphMLReader extends AbstractGraphMLReader {

	private SampledSpatialGraphBuilder builder = new SampledSpatialGraphBuilder();
	
	@Override
	public SampledSpatialGraph readGraph(String file) {
		return (SampledSpatialGraph) super.readGraph(file);
	}
	
	@Override
	protected SampledSpatialEdge addEdge(SparseVertex v1, SparseVertex v2,
			Attributes attrs) {
		return builder.addEdge((SampledSpatialGraph)graph, (SampledSpatialVertex)v1, (SampledSpatialVertex)v2);
	}

	@Override
	protected SampledSpatialVertex addVertex(Attributes attrs) {
		double x = Double.parseDouble(attrs.getValue(SpatialGraphMLReader.COORD_X_TAG));
		double y = Double.parseDouble(attrs.getValue(SpatialGraphMLReader.COORD_Y_TAG));
		SampledSpatialVertex v = builder.addVertex((SampledSpatialGraph)graph, new CoordImpl(x, y));
		
		String str = attrs.getValue(SampledSpatialGraphMLWriter.DETECTED_TAG);
		if(str != null)
			v.detect(Integer.parseInt(str));
		
		str = attrs.getValue(SampledSpatialGraphMLWriter.SAMPLED_TAG);
		if(str != null)
			v.sample(Integer.parseInt(str));
		
		return v;
	}

	@Override
	protected SampledSpatialGraph newGraph(Attributes attrs) {
		return builder.createGraph();
	}

}
