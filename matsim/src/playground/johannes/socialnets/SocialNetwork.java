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
package playground.johannes.socialnets;

import java.util.Set;

import org.matsim.population.Person;

import playground.johannes.graph.AbstractSparseGraph;
import playground.johannes.graph.Edge;
import playground.johannes.graph.SparseEdge;

/**
 * @author illenberger
 *
 */
public class SocialNetwork extends AbstractSparseGraph {
	
	public Ego addEgo(Person person) {
		Ego e = new Ego(person);
		if(insertVertex(e))
			return e;
		else
			return null;
	}
	
	public Edge addEdge(Ego e1, Ego e2) {
		SparseEdge e = new SparseEdge(e1, e2);
		if(insertEdge(e, e1, e2))
			return e;
		else
			return null;
	}
	
	@SuppressWarnings("unchecked")
	public Set<? extends Ego> getVertices() {
		return (Set<? extends Ego>) super.getVertices();
	}

}
