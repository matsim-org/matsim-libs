/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkFactory.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.social;

import java.util.Iterator;

import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPopulation;

import playground.johannes.socialnetworks.graph.GraphFactory;

/**
 * @author illenberger
 *
 */
public class SocialNetworkFactory<P extends BasicPerson<?>> implements GraphFactory<SocialNetwork<P>, Ego<P>, SocialTie> {

	private BasicPopulation<P> population;
	
	private Iterator<P> popIterator;
	
	public SocialNetworkFactory(BasicPopulation<P> population) {
		this.population = population;
	}
	
	public SocialTie addEdge(SocialNetwork<P> g, Ego<P> v1, Ego<P> v2) {
		SocialTie tie = new SocialTie(v1, v2);
		if(g.insertEdge(tie, v1, v2))
			return tie;
		else
			return null;
		
	}

	public SocialTie addEdge(SocialNetwork<P> g, Ego<P> v1, Ego<P> v2, int created) {
		SocialTie tie = new SocialTie(v1, v2, created);
		if(g.insertEdge(tie, v1, v2))
			return tie;
		else
			return null;
	}
	
	public Ego<P> addVertex(SocialNetwork<P> g) {
		if(popIterator.hasNext()) {
			return addVertex(g, popIterator.next());
		} else
			return null;
	}
	
	public Ego<P> addVertex(SocialNetwork<P> g, P person) {
		Ego<P> ego = new Ego<P>(person);
		if(g.insertVertex(ego))
			return ego;
		else
			return null;
	}

	public SocialNetwork<P> createGraph() {
		popIterator = population.getPersons().values().iterator();
		return new SocialNetwork<P>();
	}

}
