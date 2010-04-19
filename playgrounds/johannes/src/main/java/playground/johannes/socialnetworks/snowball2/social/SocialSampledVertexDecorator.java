/* *********************************************************************** *
 * project: org.matsim.*
 * SocialSampledVertexDecorator.java
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
package playground.johannes.socialnetworks.snowball2.social;

import java.util.List;

import org.matsim.api.core.v01.Coord;

import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class SocialSampledVertexDecorator<V extends SocialVertex> extends SampledVertexDecorator<V> implements SocialVertex {

	protected SocialSampledVertexDecorator(V delegate) {
		super(delegate);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SocialSampledEdgeDecorator<?>> getEdges() {
		return (List<? extends SocialSampledEdgeDecorator<?>>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SocialSampledVertexDecorator<V>> getNeighbours() {
		return (List<? extends SocialSampledVertexDecorator<V>>) super.getNeighbours();
	}

	@Override
	public SocialPerson getPerson() {
		return getDelegate().getPerson();
	}

	/**
	 * @deprecated
	 */
	@Override
	public Coord getCoordinate() {
		return getDelegate().getCoordinate();
	}

	@Override
	public Point getPoint() {
		return getDelegate().getPoint();
	}

}
