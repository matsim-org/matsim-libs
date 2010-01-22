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
package playground.johannes.socialnetworks.graph.social;

import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;


/**
 * @author illenberger
 *
 */
public class SocialNetworkBuilder extends AbstractSparseGraphBuilder<SocialNetwork, Ego, SocialTie>{

	public SocialNetworkBuilder() {
		super(new SocialNetworkFactory());
	}

	@Override
	public Ego addVertex(SocialNetwork graph) {
		throw new UnsupportedOperationException();
	}
	
	public Ego addVertex(SocialNetwork graph, SocialPerson person) {
		Ego ego = ((SocialNetworkFactory)getFactory()).createVertex(person);
		if(insertVertex(graph, ego))
			return ego;
		else
			return null;
	}
	
	public SocialTie addEdge(SocialNetwork graph, Ego vertex1, Ego vertex2, int created) {
		SocialTie edge = ((SocialNetworkFactory)getFactory()).createEdge(created);
		if(insertEdge(graph, vertex1, vertex2, edge))
			return edge;
		else
			return null;
	}
}
