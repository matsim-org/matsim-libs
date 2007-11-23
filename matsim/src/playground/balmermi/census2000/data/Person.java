/* *********************************************************************** *
 * project: org.matsim.*
 * Person.java
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



public class Person implements Comparable<Person> {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final Integer p_id;
	protected final Household hh;
	protected int age;
	protected boolean male;
	protected boolean swiss;
	protected boolean employed;
	protected String car_avail;
	protected boolean license;
	protected int curr_educ;
	protected int passed_educ;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Person(Integer p_id, Household hh) {
		this.p_id = p_id;
		this.hh = hh;
	}
	
	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public int compareTo(Person other) {
		if (this.p_id < other.p_id) { return -1; }
		else if (this.p_id > other.p_id) { return 1; }
		else { return 0; }
	}

	public final int getAge() { return this.age; }
	public final String getSex() { if (male) { return "m"; } else { return "f"; }}
	public final boolean isMale() { return this.male; }
	public final String getLicense() { if (license) { return "yes"; } else { return "no"; }}
	public final boolean hasLicense() { return this.license; }
	public final String getCarAvail() { return car_avail; }
	public final String getEmployed() { if (employed) { return "yes"; } else { return "no"; }}
	public final Household getHousehold() { return this.hh; }
	public final boolean isSwiss() { return this.swiss; }
	
	//////////////////////////////////////////////////////////////////////
	
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[p_id=" + this.p_id + "]" +
			"[hh_id=" + this.hh.hh_id + "]" +
			"[age=" + this.age + "]" +
			"[male=" + this.male + "]" +
			"[swiss=" + this.swiss + "]" +
			"[employed=" + this.employed + "]" +
			"[car_avail=" + this.car_avail + "]" +
			"[license=" + this.license + "]" +
			"[curr_educ=" + this.curr_educ + "]" +
			"[passed_educ=" + this.passed_educ + "]";
	}
}
