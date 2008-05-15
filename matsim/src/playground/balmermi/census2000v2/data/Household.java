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

package playground.balmermi.census2000v2.data;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facility;

import playground.balmermi.census2000.data.Municipality;

public class Household {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Id id;
	private final Municipality municipality;
	private final Facility facility;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Household(final Id id, final Municipality municipality, final Facility facility) {
		this.id = id;
		this.municipality = municipality;
		this.facility = facility;
	}
	
	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public final Id getId() {
		return this.id;
	}
	
	public final Municipality getMunicipality() {
		return this.municipality;
	}
	
	public final Facility getFacility() {
		return this.facility;
	}
	
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[id=" + this.id + "]" +
			"[muni_id=" + this.municipality.getId() + "]" +
			"[fac_id=" + this.facility.getId() + "]";
	}
}
