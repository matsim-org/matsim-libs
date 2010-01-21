/* *********************************************************************** *
 * project: org.matsim.*
 * KMLVertexDescriptor.java
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
package org.matsim.contrib.sna.graph.spatial.io;

import gnu.trove.TObjectDoubleHashMap;
import net.opengis.kml._2.PlacemarkType;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

/**
 * A KMLVertexDescriptor adds a description to the placemark representing a
 * vertex with information about the following statistical properties:
 * <ul>
 * <li>degree</li>
 * </ul>
 * 
 * @author jillenberger
 * 
 */
public class KMLVertexDescriptor implements KMLObjectDetail<SpatialVertex> {

	private final TObjectDoubleHashMap<Vertex> kDistr;

	/**
	 * Creates a new vertex descriptor.
	 * 
	 * @param graph a graph
	 */
	public KMLVertexDescriptor(Graph graph) {
		kDistr = new Degree().values(graph.getVertices());
	}

	/**
	 * Adds a description to <tt>kmlPlacemark</tt> with information about
	 * statistical properties of <tt>vertex</tt>.
	 * 
	 * @see {@link KMLObjectDetail#addDetail(PlacemarkType, Object)}
	 */
	@Override
	public void addDetail(PlacemarkType kmlPlacemark, SpatialVertex vertex) {
		StringBuilder builder = new StringBuilder();

		builder.append("k = ");
		builder.append(String.valueOf(kDistr.get(vertex)));

		kmlPlacemark.setDescription(builder.toString());
	}

}
