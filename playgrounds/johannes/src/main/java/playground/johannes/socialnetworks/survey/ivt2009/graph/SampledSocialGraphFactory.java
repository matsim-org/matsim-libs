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

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.GraphFactory;
import org.matsim.core.population.PersonImpl;

import playground.johannes.socialnetworks.graph.social.SocialPerson;

import com.vividsolutions.jts.geom.Point;


/**
 * @author illenberger
 *
 */
public class SampledSocialGraphFactory implements GraphFactory<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> {

	public SampledSocialEdge createEdge() {
		return new SampledSocialEdge();
	}

	public SampledSocialGraph createGraph() {
		return new SampledSocialGraph(null);
	}

	public SampledSocialVertex createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public SampledSocialVertex createVertex(Person person, Point point) {
		return new SampledSocialVertex(new SocialPerson((PersonImpl) person), point);
	}

}
