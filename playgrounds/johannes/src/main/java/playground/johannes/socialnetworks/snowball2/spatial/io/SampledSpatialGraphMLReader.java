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
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.sna.snowball.io.SampledGraphML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;

import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraphBuilder;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseEdge;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseGraph;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseVertex;


/**
 * Extension to {@link AbstractGraphMLReader} that accounts for spatial
 * information such as the {@link SpatialGraphMLReader} and snowball sampling
 * information. Parses the {@link SampledGraphML#DETECTED_ATTR} attribute and
 * {@link SampledGraphML#SAMPLED_ATTR} attribute.
 * 
 * @author illenberger
 * 
 */
public class SampledSpatialGraphMLReader extends AbstractGraphMLReader<SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseEdge> {

	private SampledSpatialGraphBuilder builder;
	
	/**
	 * @see {@link AbstractGraphMLReader#readGraph(String)}
	 */
	@Override
	public SampledSpatialSparseGraph readGraph(String file) {
		return (SampledSpatialSparseGraph) super.readGraph(file);
	}
	
	/**
	 * @see {@link SampledSpatialGraphBuilder#addEdge(SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseVertex)}
	 */
	@Override
	protected SampledSpatialSparseEdge addEdge(SampledSpatialSparseVertex v1, SampledSpatialSparseVertex v2,
			Attributes attrs) {
		return builder.addEdge((SampledSpatialSparseGraph)getGraph(), (SampledSpatialSparseVertex)v1, (SampledSpatialSparseVertex)v2);
	}

	/**
	 * Parses the attributes data and adds a new vertex with spatial and sampled
	 * information to the graph.
	 * 
	 * @param attrs the attributes data.
	 */
	@Override
	protected SampledSpatialSparseVertex addVertex(Attributes attrs) {
		SampledSpatialSparseVertex v = builder.addVertex((SampledSpatialSparseGraph)getGraph(), SpatialGraphML.newPoint(attrs));
		
		SampledGraphML.applyDetectedState(v, attrs);
		SampledGraphML.applySampledState(v, attrs);
		
		return v;
	}

	/**
	 * Creates and returns a new sampled spatial sparse graph with its
	 * coordinate reference system set to the SRID in the attributes data.
	 * 
	 * @param attrs the attributes data.
	 */
	@Override
	protected SampledSpatialSparseGraph newGraph(Attributes attrs) {
		CoordinateReferenceSystem crs = SpatialGraphML.newCRS(attrs); 
		builder = new SampledSpatialGraphBuilder(crs);
		return builder.createGraph();
	}

}
