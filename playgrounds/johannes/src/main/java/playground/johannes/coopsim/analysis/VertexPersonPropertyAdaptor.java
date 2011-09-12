/* *********************************************************************** *
 * project: org.matsim.*
 * VertexPersonPropertyAdaptor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AbstractVertexProperty;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class VertexPersonPropertyAdaptor extends AbstractVertexProperty {

	private final Map<Person, Vertex> personVertexMap;
	
	private final PersonProperty delegate;
	
	public VertexPersonPropertyAdaptor(SocialGraph graph, PersonProperty delegate) {
		personVertexMap = new HashMap<Person, Vertex>(graph.getVertices().size());
		for(SocialVertex v : graph.getVertices()) {
			personVertexMap.put(v.getPerson().getPerson(), v);
		}
		this.delegate = delegate;
	}
	
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		@SuppressWarnings("unchecked")
		Set<SocialVertex> socialVertices = (Set<SocialVertex>) vertices;
		
		Set<Person> personSet = new HashSet<Person>(vertices.size());
		for(SocialVertex v : socialVertices) {
			personSet.add(v.getPerson().getPerson());
		}
		
		TObjectDoubleHashMap<Person> pValues = delegate.values(personSet);
		TObjectDoubleHashMap<Vertex> vValues = new TObjectDoubleHashMap<Vertex>(pValues.size());
		
		TObjectDoubleIterator<Person> it = pValues.iterator();
		for(int i = 0; i< pValues.size(); i++) {
			it.advance();
			vValues.put(personVertexMap.get(it.key()), it.value());
		}
		
		return vValues;
	}

}
