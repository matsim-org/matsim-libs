/* *********************************************************************** *
 * project: org.matsim.*
 * Ego.java
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

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;

/**
 * @author illenberger
 *
 */
public class Ego<P extends Person> extends SpatialSparseVertex {

	private P person;
	
	protected Ego(P person) {
		super(((Activity) person.getPlans().get(0).getPlanElements().get(0)).getCoord());
		this.person = person;
	}
	
	public P getPerson() {
		return person;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends Ego<P>> getNeighbours() {
		return (List<? extends Ego<P>>) super.getNeighbours();
	}
}
