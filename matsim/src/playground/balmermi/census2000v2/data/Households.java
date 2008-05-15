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

package playground.balmermi.census2000v2.data;

import java.util.HashMap;

import org.matsim.basic.v01.Id;

import playground.balmermi.census2000.data.Municipalities;

public class Households {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final HashMap<Id,Household> households = new HashMap<Id, Household>();
	private final Municipalities municipalities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Households(Municipalities municipalities) {
		super();
		this.municipalities = municipalities;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Household getHousehold(final Id id) {
		return this.households.get(id);
	}
	
	public final HashMap<Id,Household> getHouseholds() {
		return this.households;
	}

	//////////////////////////////////////////////////////////////////////
	// set/create methods
	//////////////////////////////////////////////////////////////////////
	
	public final void setHH(Household hh) {
		this.households.put(hh.getId(),hh);
	}

	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[nof_munis=" + this.municipalities.getMunicipalities().size() + "]" +
			"[nof_hhs=" + this.households.size() + "]";
	}
}
