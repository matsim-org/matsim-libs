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
package playground.johannes.socialnetworks.snowball2.spatial.io;

import org.matsim.contrib.sna.graph.io.AbstractGraphMLReader;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;

import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraphBuilder;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseEdge;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseGraph;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseVertex;

/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphMLReader extends AbstractGraphMLReader<SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseEdge> {

	private SampledSpatialGraphBuilder builder;
	
//	private final GeometryFactory geometryFactory;
	
	public SampledSpatialGraphMLReader(int SRID) {
//		geometryFactory = new GeometryFactory();
//		builder = new SampledSpatialGraphBuilder(CRSUtils.getCRS(SRID));
	}
	
	@Override
	public SampledSpatialSparseGraph readGraph(String file) {
		return (SampledSpatialSparseGraph) super.readGraph(file);
	}
	
	@Override
	protected SampledSpatialSparseEdge addEdge(SampledSpatialSparseVertex v1, SampledSpatialSparseVertex v2,
			Attributes attrs) {
		return builder.addEdge((SampledSpatialSparseGraph)getGraph(), (SampledSpatialSparseVertex)v1, (SampledSpatialSparseVertex)v2);
	}

	@Override
	protected SampledSpatialSparseVertex addVertex(Attributes attrs) {
//		double x = Double.parseDouble(attrs.getValue(SpatialGraphMLHelper.COORD_X_TAG));
//		double y = Double.parseDouble(attrs.getValue(SpatialGraphMLHelper.COORD_Y_TAG));
		SampledSpatialSparseVertex v = builder.addVertex((SampledSpatialSparseGraph)getGraph(), SpatialGraphML.newPoint(attrs));
		
		String str = attrs.getValue(SampledSpatialGraphMLWriter.DETECTED_TAG);
		if(str != null)
			v.detect(Integer.parseInt(str));
		
		str = attrs.getValue(SampledSpatialGraphMLWriter.SAMPLED_TAG);
		if(str != null)
			v.sample(Integer.parseInt(str));
		
		return v;
	}

	@Override
	protected SampledSpatialSparseGraph newGraph(Attributes attrs) {
		CoordinateReferenceSystem crs = SpatialGraphML.newCRS(attrs); 
		builder = new SampledSpatialGraphBuilder(crs);
		return builder.createGraph();
	}

}
