/* *********************************************************************** *
 * project: org.matsim.*
 * Household.java
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

import org.matsim.api.core.v01.Id;

import playground.balmermi.census2000v2.data.Household;

public class Human {

	private class WHousehold {
		private final Household hh = null;
	}
	
	private class ZHousehold {
		private final Household hh = null;
	}
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Id id;
	private Household hh_z = null;
	private Household hh_w = null;
	
	public int age;
	public boolean is_male;
	public boolean is_swiss;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Human(final Id id) {
		this.id = id;
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Id getId() {
		return this.id;
	}
	
	//////////////////////////////////////////////////////////////////////
	// get/set methods
	//////////////////////////////////////////////////////////////////////
	
	public final Household getHouseholdZ() {
		return this.hh_z;
	}
	
	public final Household getHouseholdW() {
		return this.hh_w;
	}
	
	//////////////////////////////////////////////////////////////////////

	public final void setHouseholdZ(Household household) {
		this.hh_z = household;
	}

	public final void setHouseholdW(Household household) {
		this.hh_w = household;
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[id=" + this.id + "]" +
			"[hhW_id=" + this.hh_w.getId() + "]" +
			"[hhZ_id=" + this.hh_w.getId() + "]";
	}
}
