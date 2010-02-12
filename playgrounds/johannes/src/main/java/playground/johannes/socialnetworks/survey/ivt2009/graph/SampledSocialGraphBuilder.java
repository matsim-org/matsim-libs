/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSocialNetFactory.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph;

import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.socialnetworks.graph.social.SocialPerson;

import com.vividsolutions.jts.geom.Point;


/**
 * @author illenberger
 *
 */
public class SampledSocialGraphBuilder extends AbstractSparseGraphBuilder<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> {

	public SampledSocialGraphBuilder(CoordinateReferenceSystem crs) {
		super(new SampledSocialGraphFactory(crs));
	}
	
	public SampledSocialGraph createGraph() {
		return ((SampledSocialGraphFactory)getFactory()).createGraph();
	}

	@Override
	public SampledSocialVertex addVertex(SampledSocialGraph g) {
		throw new UnsupportedOperationException("Use addVertex(SampledSocialGraph, SocialPerson, Point) instead.");
	}
	
	public SampledSocialVertex addVertex(SampledSocialGraph graph, SocialPerson person, Point point) {
		SampledSocialVertex vertex = ((SampledSocialGraphFactory)getFactory()).createVertex(person, point);
		if(insertVertex(graph, vertex))
			return vertex;
		else
			return null;
	}

}
