/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkBuilder.java
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
package playground.johannes.socialnetworks.sim;

import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;

import playground.johannes.socialnetworks.graph.social.SocialPerson;


/**
 * @author illenberger
 *
 */
public class SimSocialGraphBuilder extends AbstractSparseGraphBuilder<SimSocialGraph, SimSocialVertex, SimSocialEdge>{

	public SimSocialGraphBuilder() {
		super(new SimSocialGraphFactory());
	}

	@Override
	public SimSocialVertex addVertex(SimSocialGraph graph) {
		throw new UnsupportedOperationException();
	}
	
	public SimSocialVertex addVertex(SimSocialGraph graph, SocialPerson person) {
		SimSocialVertex ego = ((SimSocialGraphFactory)getFactory()).createVertex(person);
		if(insertVertex(graph, ego))
			return ego;
		else
			return null;
	}
	
	public SimSocialEdge addEdge(SimSocialGraph graph, SimSocialVertex vertex1, SimSocialVertex vertex2, int created) {
		SimSocialEdge edge = ((SimSocialGraphFactory)getFactory()).createEdge(created);
		if(insertEdge(graph, vertex1, vertex2, edge))
			return edge;
		else
			return null;
	}
}
