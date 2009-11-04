/* *********************************************************************** *
 * project: org.matsim.*
 * SNKMLDegreeStyle.java
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

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;

import gnu.trove.TDoubleObjectHashMap;
import net.opengis.kml._2.LinkType;
import playground.johannes.socialnetworks.graph.GraphStatistics;

/**
 * @author illenberger
 *
 */
public class KMLDegreeStyle extends KMLVertexColorStyle<Graph, Vertex> {

	private static final String VERTEX_STYLE_PREFIX = "vertex.style.";
	
	public KMLDegreeStyle(LinkType vertexIconLink) {
		super(vertexIconLink);
	}

	@Override
	protected TDoubleObjectHashMap<String> getValues(Graph graph) {
		double[] degrees = GraphStatistics.degreeDistribution(graph).absoluteDistribution().keys();
		TDoubleObjectHashMap<String> values = new TDoubleObjectHashMap<String>();
		for(double k : degrees) {
			values.put(k, VERTEX_STYLE_PREFIX + Integer.toString((int)k));
		}
		
		return values;
	}

//	private String getVertexStyleId(int k) {
//		return ;
//	}

	@Override
	protected double getValue(Vertex vertex) {
		return vertex.getEdges().size();
	}
}
