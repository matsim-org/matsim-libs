/* *********************************************************************** *
 * project: org.matsim.*
 * Households.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.census2000v2.old;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;


public class Humans {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final HashMap<Id,Human> humans = new HashMap<Id,Human>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Humans() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Human getHuman(final Id id) {
		return this.humans.get(id);
	}
	
	public final HashMap<Id,Human> getHumans() {
		return this.humans;
	}

	//////////////////////////////////////////////////////////////////////
	// set/create methods
	//////////////////////////////////////////////////////////////////////
	
	public final void addHuman(Human xyz) {
		this.humans.put(xyz.getId(),xyz);
	}

	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[nof_humans=" + this.humans.size() + "]";
	}
}
