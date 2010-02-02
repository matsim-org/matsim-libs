/* *********************************************************************** *
 * project: org.matsim.*
 * SampledEgo.java
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.SnowballAttributes;

import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class SampledSocialVertex extends SpatialSparseVertex implements SocialVertex, SampledVertex {

	private SocialPerson person;
	
	private SnowballAttributes attributes;
	
	private List<SampledSocialVertex> sources;
	
	protected SampledSocialVertex(SocialPerson person, Point point) {
		super(point);
		this.person = person;
		attributes = new SnowballAttributes();
		sources = new ArrayList<SampledSocialVertex>(3);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<? extends SampledSocialEdge> getEdges() {
		return (List<? extends SampledSocialEdge>) super.getEdges();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<? extends SampledSocialVertex> getNeighbours() {
		return (List<? extends SampledSocialVertex>) super.getNeighbours();
	}

	public void detect(int iteration) {
		attributes.detect(iteration);
	}

	public int getIterationDetected() {
		return attributes.getIterationDeteted();
	}

	public int getIterationSampled() {
		return attributes.getIterationSampled();
	}

	public boolean isDetected() {
		return attributes.isDetected();
	}

	public boolean isSampled() {
		return attributes.isSampled();
	}

	public void sample(int iteration) {
		attributes.sample(iteration);
	}

	public void addSource(SampledSocialVertex vertex) {
		sources.add(vertex);
	}
	
	public List<SampledSocialVertex> getSources() {
		return sources;
	}

	@Override
	public SocialPerson getPerson() {
		return person;
	}
}
