/* *********************************************************************** *
 * project: org.matsim.*
 * SampledEdge.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.snowball;

import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.EdgeDecorator;

/**
 * @author illenberger
 *
 */
public class SampledEdge extends SparseEdge {

	private EdgeDecorator<SampledEdge> projection;
	
	protected SampledEdge() {
		
	}

	@Override
	public SampledVertex getOpposite(Vertex v) {
		return (SampledVertex) super.getOpposite(v);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Tuple<SampledVertex, SampledVertex> getVertices() {
		return (Tuple<SampledVertex, SampledVertex>) super.getVertices();
	}
	
	void setProjection(EdgeDecorator<SampledEdge> projection) {
		this.projection = projection;
	}
	
	public EdgeDecorator<SampledEdge> getProjection() {
		return projection;
	}
	
	void reset() {
		projection = null;
	}
}
