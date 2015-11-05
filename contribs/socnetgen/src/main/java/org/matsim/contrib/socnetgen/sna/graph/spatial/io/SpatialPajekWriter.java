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
package org.matsim.contrib.socnetgen.sna.graph.spatial.io;

import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.socnetgen.sna.graph.io.PajekAttributes;
import org.matsim.contrib.socnetgen.sna.graph.io.PajekColorizer;
import org.matsim.contrib.socnetgen.sna.graph.io.PajekWriter;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class SpatialPajekWriter extends PajekWriter<SpatialGraph, SpatialVertex, SpatialEdge>
		implements PajekAttributes<SpatialVertex, SpatialEdge> {

	private PajekColorizer<SpatialVertex, SpatialEdge> colorizer;

	@SuppressWarnings("unchecked")
	public void write(SpatialGraph graph, PajekColorizer<? extends SpatialVertex, ? extends SpatialEdge> colorizer, String file) throws IOException {
		this.colorizer = (PajekColorizer<SpatialVertex, SpatialEdge>) colorizer;
		super.write(graph, this, file);
	}
	
	@Override
	protected String getVertexX(SpatialVertex v) {
		Point p = v.getPoint();
		if(p == null)
			return "0";
		else
			return String.valueOf(v.getPoint().getX());
	}

	@Override
	protected String getVertexY(SpatialVertex v) {
		Point p = v.getPoint();
		if(p == null)
			return "0";
		else
			return String.valueOf(v.getPoint().getY());
	}

	@Override
	public void write(SpatialGraph g, String file) throws IOException {
		colorizer = new DefaultColorizer();
		super.write(g, this, file);
	}

	public List<String> getEdgeAttributes() {
		return new ArrayList<String>();
	}

	public String getEdgeValue(SpatialEdge e, String attribute) {
		return null;
	}

	public List<String> getVertexAttributes() {
		List<String> attrs = new ArrayList<String>();
		attrs.add(PajekAttributes.VERTEX_FILL_COLOR);
		return attrs;
	}

	public String getVertexValue(SpatialVertex v, String attribute) {
		if (PajekAttributes.VERTEX_FILL_COLOR.equals(attribute))
			return colorizer.getVertexFillColor(v);
		else
			return null;
	}

	private class DefaultColorizer extends PajekColorizer<SpatialVertex, SpatialEdge> {

		private static final String COLOR_BLACK = "13";

		@Override
		public String getVertexFillColor(SpatialVertex v) {
			return COLOR_BLACK;
		}

		@Override
		public String getEdgeColor(SpatialEdge e) {
			return COLOR_BLACK;
		}
		
	}
}
