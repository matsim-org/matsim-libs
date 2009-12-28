/* *********************************************************************** *
 * project: org.matsim.*
 * Intersection.java
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

package playground.balmermi.lsa;

import java.util.HashMap;

import org.matsim.core.gbl.Gbl;

public class Intersection implements Comparable<Intersection>  {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final Integer id;
	protected final String desc;
	protected final HashMap<Integer,LSA> lsas = new HashMap<Integer,LSA>();
	protected final HashMap<Integer,Lane> lanes = new HashMap<Integer,Lane>();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Intersection(Integer id, String desc) {
		this.id = id;
		this.desc = desc;
	}
	
	//////////////////////////////////////////////////////////////////////
	// set/add methods
	//////////////////////////////////////////////////////////////////////

	public final void addLSA(LSA lsa) {
		if (this.lsas.containsKey(lsa.nr)) { Gbl.errorMsg("Intersection_id=" + this.id + ": LSA_nr=" + lsa.nr + " already exists!"); }
		this.lsas.put(lsa.nr,lsa);
	}
	
	public final void addLane(Lane lane) {
		if (this.lanes.containsKey(lane.nr)) { Gbl.errorMsg("Intersection_id=" + this.id + ": lane_nr=" + lane.nr + " already exists!"); }
		this.lanes.put(lane.nr,lane);
	}
	
	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public int compareTo(Intersection other) {
		if (this.id < other.id) { return -1; }
		else if (this.id > other.id) { return 1; }
		else { return 0; }
	}

	@Override
	public final String toString() {
		return "[id=" + this.id + "]" +
			"[desc=" + this.desc + "]" +
			"[nof_LSAs=" + this.lsas.size() + "]" +
			"[nof_lanes=" + this.lanes.size() + "]";
	}
}
