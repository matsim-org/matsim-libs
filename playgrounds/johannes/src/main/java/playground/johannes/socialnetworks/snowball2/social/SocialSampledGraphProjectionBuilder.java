/* *********************************************************************** *
 * project: org.matsim.*
 * SocialSampledGraphProjectionBuilder.java
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

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjectionBuilder;

/**
 * @author illenberger
 *
 */
public class SocialSampledGraphProjectionBuilder<G extends SocialGraph, V extends SocialVertex, E extends SocialEdge> extends SampledGraphProjectionBuilder<G, V, E> {

	public SocialSampledGraphProjectionBuilder() {
		super(new SocialSampledGraphProjectionFactory<G, V, E>());
	}
}
