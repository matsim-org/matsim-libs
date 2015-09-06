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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.balmermi.census2000.data.Municipality;

public class Household {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(Household.class);

	private final Id<Household> id;
	private final Municipality municipality;
	private final ActivityFacilityImpl facility;
	private final Map<Id<Person>,Person> persons_w = new HashMap<Id<Person>,Person>();
	private final Map<Id<Person>,Person> persons_z = new HashMap<Id<Person>,Person>();
	private int hhtpz = Integer.MIN_VALUE;
	private int hhtpw = Integer.MIN_VALUE;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Household(final Id<Household> id, final Municipality municipality, final ActivityFacilityImpl facility) {
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
	// remove methods
	//////////////////////////////////////////////////////////////////////

	public final Person removePersonW(Id<Person> pid) {
		return this.persons_w.remove(pid);
	}
	
	public final Person removePersonZ(Id<Person> pid) {
		return this.persons_z.remove(pid);
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Id<Household> getId() {
		return this.id;
	}
	
	public final Municipality getMunicipality() {
		return this.municipality;
	}
	
	public final ActivityFacilityImpl getFacility() {
		return this.facility;
	}
	
	public final Map<Id<Person>,Person> getPersonsW() {
		return this.persons_w;
	}
	
	public final Map<Id<Person>,Person> getPersonsZ() {
		return this.persons_z;
	}
	
	public final Map<Id<Person>,Person> getPersons() {
		Map<Id<Person>,Person> map = new HashMap<>(this.persons_w);
		map.putAll(this.persons_z);
		return map;
	}
	
	public final Map<Id<Person>,Person> getKidsW() {
		Map<Id<Person>,Person> map = new HashMap<>();
		for (Person p : this.persons_w.values()) { if (PersonUtils.getAge(p) < 15) { map.put(p.getId(),p); } }
		return map;
	}
	
	public final Map<Id<Person>,Person> getKidsZ() {
		Map<Id<Person>,Person> map = new HashMap<Id<Person>, Person>();
		for (Person p : this.persons_z.values()) { if (PersonUtils.getAge(p) < 15) { map.put(p.getId(),p); } }
		return map;
	}

	public final Map<Id<Person>,Person> getKids() {
		Map<Id<Person>,Person> map = new HashMap<Id<Person>, Person>(this.getKidsW());
		map.putAll(this.getKidsZ());
		return map;
	}
	
	public final double getKidsWFraction() {
		return ((double)(this.getKidsW().size()))/((double)(this.getPersonsW().size()));
	}
	
	public final double getKidsZFraction() {
		return ((double)(this.getKidsZ().size()))/((double)(this.getPersonsZ().size()));
	}
	
	public final double getKidsFraction() {
		return ((double)(this.getKids().size()))/((double)(this.getPersons().size()));
	}
	
	public final int getHHTPW() {
		return this.hhtpw;
	}
	
	public final int getHHTPZ() {
		return this.hhtpz;
	}
	
	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////


	@Override
	public final String toString() {
		return this.id.toString();
	}
}
