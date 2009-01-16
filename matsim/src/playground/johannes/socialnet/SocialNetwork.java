/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.socialnet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.population.Person;
import org.matsim.population.Population;

import playground.johannes.graph.AbstractSparseGraph;
import playground.johannes.graph.SparseVertex;

/**
 * @author illenberger
 *
 */
public class SocialNetwork extends AbstractSparseGraph {
	
	private Map<Person, Ego> personEgoMapping = new HashMap<Person, Ego>();
	
	public SocialNetwork() {
		super();
	}
	
	public SocialNetwork(Population pop) {
		this();
		for(Person p : pop.getPersons().values())
			addEgo(p);
	}
	
	public Ego addEgo(Person person) {
		Ego e = new Ego(person);
		if(insertVertex(e)) {
			personEgoMapping.put(person, e);
			return e;
		} else
			return null;
	}
	
	public SocialTie addEdge(Ego e1, Ego e2) {
		return this.addEdge(e1, e2, 0);
	}
	
	public SocialTie addEdge(Ego e1, Ego e2, int created) {
		SocialTie e = new SocialTie(e1, e2, created);
		if(insertEdge(e, e1, e2))
			return e;
		else
			return null;
	}
	
	@SuppressWarnings("unchecked")
	public Set<? extends Ego> getVertices() {
		return (Set<? extends Ego>) super.getVertices();
	}
	
	public Ego getEgo(Person p) {
		return personEgoMapping.get(p);
	}

	@Override
	public SocialTie getEdge(SparseVertex v1, SparseVertex v2) {
		return (SocialTie) super.getEdge(v1, v2);
	}

}
