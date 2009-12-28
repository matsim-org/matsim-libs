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
package playground.johannes.socialnetworks.graph.social;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.SparseVertex;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;

/**
 * @author illenberger
 *
 */
public class SocialNetwork<P extends Person> extends SpatialSparseGraph {
	
	private Map<P, Ego<P>> personEgoMapping = new HashMap<P, Ego<P>>();
	
	public SocialNetwork() {
		super();
	}
	
//	@Deprecated
//	public SocialNetwork(BasicPopulation<P> pop) {
//		this();
//		for(P p : pop.getPersons().values())
//			addEgo(p);
//	}
//	
//	@Deprecated
//	public Ego<P> addEgo(P person) {
//		Ego<P> e = new Ego<P>(person);
//		if(insertVertex(e)) {
//			return e;
//		} else
//			return null;
//	}
//	
//	@Deprecated
//	public SocialTie addEdge(Ego<P> e1, Ego<P> e2) {
//		return this.addEdge(e1, e2, 0);
//	}
//	
//	@Deprecated
//	public SocialTie addEdge(Ego<P> e1, Ego<P> e2, int created) {
//		SocialTie e = new SocialTie(e1, e2, created);
//		if(insertEdge(e))
//			return e;
//		else
//			return null;
//	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends Ego<P>> getVertices() {
		return (Set<? extends Ego<P>>) super.getVertices();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SocialTie> getEdges() {
		return (Set<? extends SocialTie>) super.getEdges();
	}

	public Ego<P> getEgo(P p) {
		return personEgoMapping.get(p);
	}

	@Override
	public SocialTie getEdge(SparseVertex v1, SparseVertex v2) {
		return (SocialTie) super.getEdge(v1, v2);
	}

//	@Override
//	protected boolean insertEdge(SparseEdge e) {
//		return super.insertEdge(e);
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	protected boolean insertVertex(SparseVertex v) {
//		if(super.insertVertex(v)) {
//			personEgoMapping.put(((Ego<P>)v).getPerson(), (Ego<P>)v);
//			return true;
//		} else
//			return false;
//	}

}
