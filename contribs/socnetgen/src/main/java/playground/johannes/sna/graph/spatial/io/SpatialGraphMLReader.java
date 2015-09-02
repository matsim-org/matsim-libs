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
package playground.johannes.sna.graph.spatial.io;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;

import playground.johannes.sna.graph.io.AbstractGraphMLReader;
import playground.johannes.sna.graph.spatial.SpatialSparseEdge;
import playground.johannes.sna.graph.spatial.SpatialSparseGraph;
import playground.johannes.sna.graph.spatial.SpatialSparseGraphBuilder;
import playground.johannes.sna.graph.spatial.SpatialSparseVertex;

/**
 * An extension to {@link AbstractGraphMLReader} that includes reading spatial
 * information from spatial graphs
 * 
 * @author illenberger
 * 
 */
public class SpatialGraphMLReader
		extends
		AbstractGraphMLReader<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> {

	private SpatialSparseGraphBuilder builder;

	/**
	 * @see {@link AbstractGraphMLReader#readGraph(String)}
	 */
	@Override
	public SpatialSparseGraph readGraph(String file) {
		return (SpatialSparseGraph) super.readGraph(file);
	}

	/**
	 * @see {@link SampledSpatialGraphBuilder#addEdge(SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseVertex)}
	 */
	@Override
	protected SpatialSparseEdge addEdge(SpatialSparseVertex v1,
			SpatialSparseVertex v2, Attributes attrs) {
		return builder.addEdge(getGraph(), v1, v2);
	}

	/**
	 * Parses the attributes data and adds a new vertex with spatial information
	 * to the graph.
	 * 
	 * @param attrs
	 *            the attributes data.
	 */
	@Override
	protected SpatialSparseVertex addVertex(Attributes attrs) {
		return builder.addVertex(getGraph(),
				SpatialGraphML.newPoint(attrs));
	}

	/**
	 * Creates and returns a new spatial sparse graph with its coordinate
	 * reference system set to the SRID in the attributes data.
	 * 
	 * @param attrs
	 *            the attributes data.
	 */
	@Override
	protected SpatialSparseGraph newGraph(Attributes attrs) {
		CoordinateReferenceSystem crs = SpatialGraphML.newCRS(attrs);
		builder = new SpatialSparseGraphBuilder(crs);
		return builder.createGraph();
	}
}
