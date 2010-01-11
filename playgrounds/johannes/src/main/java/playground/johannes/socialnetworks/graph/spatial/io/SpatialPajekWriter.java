/* *********************************************************************** *
 * project: org.matsim.*
 * SNPajekWriter.java
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
import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;

import playground.johannes.socialnetworks.graph.io.PajekAttributes;
import playground.johannes.socialnetworks.graph.io.PajekColorizer;
import playground.johannes.socialnetworks.graph.io.PajekWriter;

/**
 * @author illenberger
 *
 */
public class SpatialPajekWriter extends PajekWriter<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>
		implements PajekAttributes<SpatialSparseVertex, SpatialSparseEdge> {

	private PajekColorizer<SpatialSparseVertex, SpatialSparseEdge> colorizer;

	@SuppressWarnings("unchecked")
	public void write(SpatialSparseGraph graph, PajekColorizer<? extends SpatialSparseVertex, ? extends SpatialSparseEdge> colorizer, String file) throws IOException {
		this.colorizer = (PajekColorizer<SpatialSparseVertex, SpatialSparseEdge>) colorizer;
		super.write(graph, this, file);
	}
	
	@Override
	protected String getVertexX(SpatialSparseVertex v) {
		return String.valueOf(v.getCoordinate().getX());
	}

	@Override
	protected String getVertexY(SpatialSparseVertex v) {
		return String.valueOf(v.getCoordinate().getY());
	}

	@Override
	public void write(SpatialSparseGraph g, String file) throws IOException {
		colorizer = new DefaultColorizer();
		super.write(g, this, file);
	}

	public List<String> getEdgeAttributes() {
		return new ArrayList<String>();
	}

	public String getEdgeValue(SpatialSparseEdge e, String attribute) {
		return null;
	}

	public List<String> getVertexAttributes() {
		List<String> attrs = new ArrayList<String>();
		attrs.add(PajekAttributes.VERTEX_FILL_COLOR);
		return attrs;
	}

	public String getVertexValue(SpatialSparseVertex v, String attribute) {
		if (PajekAttributes.VERTEX_FILL_COLOR.equals(attribute))
			return colorizer.getVertexFillColor(v);
		else
			return null;
	}

	private class DefaultColorizer extends PajekColorizer<SpatialSparseVertex, SpatialSparseEdge> {

		private static final String COLOR_BLACK = "13";

		@Override
		public String getVertexFillColor(SpatialSparseVertex v) {
			return COLOR_BLACK;
		}

		@Override
		public String getEdgeColor(SpatialSparseEdge e) {
			return COLOR_BLACK;
		}
		
	}
}
