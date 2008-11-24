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
package playground.johannes.snowball;

import org.matsim.utils.collections.Tuple;

import playground.johannes.graph.EdgeDecorator;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class SampledEdge extends SparseEdge {

	private EdgeDecorator<SampledEdge> projection;
	
	protected SampledEdge(SampledVertex v1, SampledVertex v2) {
		super(v1, v2);
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
