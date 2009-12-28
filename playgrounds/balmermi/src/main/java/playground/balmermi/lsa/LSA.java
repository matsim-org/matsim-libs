/* *********************************************************************** *
 * project: org.matsim.*
 * LSA.java
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
import java.util.Iterator;

import org.matsim.core.gbl.Gbl;

public class LSA implements Comparable<LSA> {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final Intersection intersection;
	protected final Integer nr;
	protected final HashMap<Integer,Lane> lanes = new HashMap<Integer, Lane>();
	private final double[] r_time = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	private final double[] g_time = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	private final int[] entry_cnt = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public LSA(Integer nr, Intersection intersection) {
		this.nr = nr;
		this.intersection = intersection;
	}
	
	//////////////////////////////////////////////////////////////////////
	// add/set methods
	//////////////////////////////////////////////////////////////////////
	
	public final void addLane(Lane lane) {
		if (this.lanes.containsKey(lane.nr)) { Gbl.errorMsg("Intersection_id=" + this.intersection.id + ", lsa_nr=" + this.nr + ": lane_nr=" + lane.nr + " already exists!"); }
		this.lanes.put(lane.nr,lane);
	}

	public final void addEntry(int bin_starttime, double rt, double gt) {
		double rtf = rt/(rt+gt);
		double gtf = gt/(rt+gt);
		int hour = bin_starttime/3600;
		hour = hour % 24;
		
		this.r_time[hour] += rtf;
		this.g_time[hour] += gtf;
		this.entry_cnt[hour]++;
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////
	
	public final double getRTF(int hour) {
		int h = hour % 24;
		return this.r_time[h]/this.entry_cnt[h];
	}

	public final double getGTF(int hour) {
		int h = hour % 24;
		return this.g_time[h]/this.entry_cnt[h];
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public int compareTo(LSA other) {
		if (this.nr < other.nr) { return -1; }
		else if (this.nr > other.nr) { return 1; }
		else { return 0; }
	}

	@Override
	public final String toString() {
		String str = "[nr=" + this.nr + "]" + "[intersec_id=" + this.intersection.id + "]" + "[#lanes=" + this.lanes.size() + "]\n";

		str = str + "  lane_nrs:  \t";
		Iterator<Lane> lane_it = this.lanes.values().iterator();
		while (lane_it.hasNext()) { Lane lane = lane_it.next(); str = str + lane.nr + "\t"; }
		str = str + "\n";

		str = str + "  r_times:   \t";
		for (int i=0; i<this.r_time.length; i++) { str = str + this.r_time[i] + "\t"; }
		str = str + "\n";

		str = str + "  g_times:   \t";
		for (int i=0; i<this.g_time.length; i++) { str = str + this.g_time[i] + "\t"; }
		str = str + "\n";

		str = str + "  entry_cnt: \t";
		for (int i=0; i<this.entry_cnt.length; i++) { str = str + this.entry_cnt[i] + "\t"; }
		str = str + "\n";

		return str;
	}
}
