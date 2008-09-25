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

import playground.johannes.graph.SparseGraph;

/**
 * @author illenberger
 *
 */
public class SocialNetwork extends SparseGraph {

	public Ego addEgo(Person person) {
		Ego e = (Ego)addVertex();
		e.setPerson(person);
		return e;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends Ego> getVertices() {
		return (Set<? extends Ego>) super.getVertices();
	}


	@Override
	protected Ego newVertex() {
		return new Ego(null);
	}
	
	protected Ego newEgo(Person person) {
		return new Ego(person);
	}

	/**
	 * 
	 */
	public SocialNetwork() {
		// TODO Auto-generated constructor stub
	}

}
