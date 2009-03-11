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
package playground.johannes.socialnet;

import java.util.List;

import org.matsim.interfaces.basic.v01.BasicPerson;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.Coord;

import playground.johannes.graph.SparseVertex;

/**
 * @author illenberger
 *
 */
public class Ego<P extends BasicPerson<BasicPlan>> extends SparseVertex {

	private P person;
	
	protected Ego(P person) {
		this.person = person;
	}
	
	public P getPerson() {
		return person;
	}
	
	public Coord getCoord() {
		return ((Activity) person.getPlans().get(0).getPlanElements().get(0)).getCoord();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends Ego<P>> getNeighbours() {
		return (List<? extends Ego<P>>) super.getNeighbours();
	}
}
