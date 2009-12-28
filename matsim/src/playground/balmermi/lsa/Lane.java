/* *********************************************************************** *
 * project: org.matsim.*
 * Lane.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;

public class Lane implements Comparable<Lane> {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final Intersection intersection;
	protected final Integer nr;
	protected final HashMap<Integer,Lane> tolanes = new HashMap<Integer, Lane>();
	protected final HashMap<Integer,LSA> lsas = new HashMap<Integer, LSA>();
	protected final HashMap<Id,LinkImpl> links = new HashMap<Id, LinkImpl>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Lane(Integer nr, Intersection intersection) {
		this.nr = nr;
		this.intersection = intersection;
	}

	//////////////////////////////////////////////////////////////////////
	// add/set methods
	//////////////////////////////////////////////////////////////////////

	public final void addToLane(Lane tolane) {
		if (this.tolanes.containsKey(tolane.nr)) { Gbl.errorMsg("Intersection_id=" + this.intersection.id + ", lane_nr=" + this.nr + ": tolane_nr=" + tolane.nr + " already exists!"); }
		this.tolanes.put(tolane.nr,tolane);
	}

	public final void addLSA(LSA lsa) {
		if (this.lsas.containsKey(lsa.nr)) { Gbl.errorMsg("Intersection_id=" + this.intersection.id + ", lane_nr=" + this.nr + ": lsa_nr=" + lsa.nr + " already exists!"); }
		this.lsas.put(lsa.nr,lsa);
	}

	public final void addLink(LinkImpl link) {
		if (this.links.containsKey(link.getId())) { Gbl.errorMsg("Intersection_id=" + this.intersection.id + ", lane_nr=" + this.nr + ": link_id=" + link.getId().toString() + " already exists!"); }
		this.links.put(link.getId(),link);
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public int compareTo(Lane other) {
		if (this.nr < other.nr) { return -1; }
		else if (this.nr > other.nr) { return 1; }
		else { return 0; }
	}

	@Override
	public final String toString() {
		return "[nr=" + this.nr + "]" +
			"[intersec_id=" + this.intersection.id + "]" +
			"[nof_tolanes=" + this.tolanes.size() + "]";
	}
}
