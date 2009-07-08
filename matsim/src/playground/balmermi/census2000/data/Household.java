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

package playground.balmermi.census2000.data;

import java.util.HashSet;
import java.util.Iterator;

import org.matsim.core.utils.geometry.CoordImpl;

public class Household implements Comparable<Household>  {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Municipality municipality;
	public Integer hh_id;
	public CoordImpl coord;
	public int hh_cat;
	public HashSet<MyPerson> persons = new HashSet<MyPerson>();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Household(Integer hh_id, Municipality municipality) {
		this.hh_id = hh_id;
		this.municipality = municipality;
	}
	
	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public int compareTo(Household other) {
		if (this.hh_id < other.hh_id) { return -1; }
		else if (this.hh_id > other.hh_id) { return 1; }
		else { return 0; }
	}

	public final boolean addPerson(MyPerson p) {
		return this.persons.add(p);
	}

	//////////////////////////////////////////////////////////////////////
	
	public final int getPersonCount() {
		return this.persons.size();
	}
	
	public final Integer getId() {
		return this.hh_id;
	}
	
	public final CoordImpl getCoord() {
		return this.coord;
	}
	
	public final int getCategory() {
		return this.hh_cat;
	}

	public final int getKidCount() {
		int cnt = 0;
		Iterator<MyPerson> p_it = this.persons.iterator();
		while (p_it.hasNext()) {
			MyPerson p = p_it.next();
			if (p.age < 15) { cnt++; }
		}
		return cnt;
	}

	public final Municipality getMunicipality() {
		return this.municipality;
	}
	
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[hh_id=" + this.hh_id + "]" +
			"[muni_id=" + this.municipality.getId() + "]" +
			"[hh_cat=" + this.hh_cat + "]" +
			"[coord=" + this.coord + "]" +
			"[kid_size=" + this.getKidCount() + "]" +
			"[p_size=" + this.getPersonCount() + "]";
	}
}
