/* *********************************************************************** *
 * project: org.matsim.*
 * SocialSampledGraphProjection.java
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
package org.matsim.contrib.socnetgen.sna.snowball.social;

import org.matsim.contrib.socnetgen.sna.graph.social.SocialEdge;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraphProjection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class SocialSampledGraphProjection<G extends SocialGraph, V extends SocialVertex, E extends SocialEdge> extends SampledGraphProjection<G, V , E> implements SocialGraph {

	public SocialSampledGraphProjection(G delegate) {
		super(delegate);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SocialSampledEdgeDecorator<E>> getEdges() {
		return (Set<? extends SocialSampledEdgeDecorator<E>>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SocialSampledVertexDecorator<V>> getVertices() {
		return (Set<? extends SocialSampledVertexDecorator<V>>) super.getVertices();
	}

	@Override
	public CoordinateReferenceSystem getCoordinateReferenceSysten() {
		return getDelegate().getCoordinateReferenceSysten();
	}

}
