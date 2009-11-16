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

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;


/**
 * @author illenberger
 *
 */
public class SocialNetworkBuilder<P extends Person> extends AbstractSparseGraphBuilder<SocialNetwork<P>, Ego<P>, SocialTie>{

	public SocialNetworkBuilder() {
		super(new SocialNetworkFactory<P>());
	}

	@Override
	public Ego<P> addVertex(SocialNetwork<P> graph) {
		throw new UnsupportedOperationException();
	}
	
	public Ego<P> addVertex(SocialNetwork<P> graph, P person) {
		Ego<P> ego = ((SocialNetworkFactory<P>)getFactory()).createVertex(person);
		if(insertVertex(graph, ego))
			return ego;
		else
			return null;
	}
	
	public SocialTie addEdge(SocialNetwork<P> graph, Ego<P> vertex1, Ego<P> vertex2, int created) {
		SocialTie edge = ((SocialNetworkFactory<P>)getFactory()).createEdge(created);
		if(insertEdge(graph, vertex1, vertex2, edge))
			return edge;
		else
			return null;
	}
}
