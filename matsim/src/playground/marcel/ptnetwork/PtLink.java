/* *********************************************************************** *
 * project: org.matsim.*
 * PtLink.java
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

package playground.marcel.ptnetwork;


import java.util.Arrays;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.network.LinkImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class PtLink extends LinkImpl{

	private TreeMap<Integer, Integer> departures = new TreeMap<Integer, Integer>();
	private int[] depTimes = null;
	private int[] travTimes = null;

	public int cost = 0;

//	private final static int UNDEFINED_TRAVELTIME = 86401;

	public PtLink(NetworkLayer network, String id, Node from, Node to, String length, String freespeed,
			String capacity, String permlanes, String origid, String type) {
		super(new Id(id), from, to, network, Double.parseDouble(length), Double.parseDouble(freespeed), Double.parseDouble(capacity), Double.parseDouble(permlanes));
	}

	/**
	 * Puts departure times and travel times to an internal structure.
	 * @param dtime - the departure of a trip
	 * @param ttime - the corresponding travel time
	 */
	public void putTtime(int dtime, int ttime){
		int dt = dtime % 86400;
		this.departures.put(Integer.valueOf(dt), Integer.valueOf(ttime));
		this.depTimes = null; // force a rebuild of the cache
		this.travTimes = null;
	}

	/**
	 * Returns travel time including wait time for specified depaturetime.
	 * For links with type != null (mostly pedestrian links), 0 is always returned.
	 * @param time the start time at node in secs from midnight
	 * @return the traveltime including waiting time, returns 0 for links with type!=null such as "P"
	 */
	public int getDynTTime(final int time) {

		if (this.depTimes == null) {
			cacheDepartureTimes();
		}

		if (this.type.equals(PtNetworkLayer.PEDESTRIAN_TYPE)) {
			return 0; // do not calc any dyn ttime for pedestrian links
		}

		int dynTTime=Integer.MAX_VALUE;

		int t  = time % 86400;

		if (t > this.depTimes[this.depTimes.length - 1]) {
			int first = this.depTimes[0];
			dynTTime = this.travTimes[0] + first + (86400 - t);
		} else {
			int index = Arrays.binarySearch(this.depTimes, t);
			if (index < 0) {
				// index = (-(insertion point) - 1), where totalSecs would be inserted
				// so take the next index from that point:
				index = - (index + 1);
			}
			int depTime = this.depTimes[index];
			int travTime = this.travTimes[index];
			dynTTime = travTime + (depTime - t);
		}

		return dynTTime;
	}

	public void setDepartures(TreeMap<Integer, Integer> departures) {
		this.departures = departures;
		cacheDepartureTimes();
	}

	/**
	 * Caches the departure and traveltimes into an optimized datastructure. The TreeMap for this.departures is rather slow,
	 * because it has to auto-unbox and -box the key values a lot of times to find the correct departure time. Storing everything
	 * in a sorted array and using binary search on it improves the overall runtime for searching routes in PtNetwork at about 25%.
	 *
	 */
	private void cacheDepartureTimes() {
		this.depTimes = new int[this.departures.size()];
		this.travTimes = new int[this.departures.size()];
		int i = 0;
		for (Integer key : this.departures.keySet()) {
			Integer value = this.departures.get(key);
			this.depTimes[i] = key.intValue();
			this.travTimes[i] = value.intValue();
			i++;
		}
	}

}
