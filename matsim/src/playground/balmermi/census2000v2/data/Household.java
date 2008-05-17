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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facility;
import org.matsim.plans.Person;

import playground.balmermi.census2000.data.Municipality;

public class Household {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(Household.class);

	private final Id id;
	private final Municipality municipality;
	private final Facility facility;
	private final Map<Id,Person> persons_w = new HashMap<Id,Person>();
	private final Map<Id,Person> persons_z = new HashMap<Id,Person>();
	private int hhtpz = Integer.MIN_VALUE;
	private int hhtpw = Integer.MIN_VALUE;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Household(final Id id, final Municipality municipality, final Facility facility) {
		this.id = id;
		this.municipality = municipality;
		this.facility = facility;
	}
	
	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final boolean setHHTPZ(int hhtpz) {
		if (hhtpz == -9) { return true; }
		if ((hhtpz < 1000) || (hhtpz > 9804)) { log.error("hhtpz val not allowed! "+ this.toString()); return false; }
		if (this.hhtpz == Integer.MIN_VALUE) { this.hhtpz = hhtpz; return true; }
		else if (this.hhtpz == hhtpz) { return true; }
		else { log.error("hhtpz val does not match! "+ this.toString()); return false; }
	}
	
	public final boolean setHHTPW(int hhtpw) {
		if (hhtpw == -9) { return true; }
		if ((hhtpw < 1000) || (hhtpw > 9804)) { log.error("hhtpw val not allowed! "+ this.toString()); return false; }
		if (this.hhtpw == Integer.MIN_VALUE) { this.hhtpw = hhtpw; return true; }
		else if (this.hhtpw == hhtpw) { return true; }
		else { log.error("hhtpw val does not match! "+ this.toString()); return false; }
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
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
	
	public final Map<Id,Person> getPersonsW() {
		return this.persons_w;
	}
	
	public final Map<Id,Person> getPersonsZ() {
		return this.persons_z;
	}
	
	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////


	@Override
	public final String toString() {
		return "[id=" + this.id + "]" +
			"[muni_id=" + this.municipality.getId() + "]" +
			"[fac_id=" + this.facility.getId() + "]" +
			"[hhtpz=" + this.hhtpz + "]" +
			"[hhtpw=" + this.hhtpw + "]" +
			"[nof_p_w=" + this.persons_w.size() + "]" +
			"[nof_p_z=" + this.persons_z.size() + "]";
	}
}
