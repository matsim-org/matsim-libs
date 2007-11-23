/* *********************************************************************** *
 * project: org.matsim.*
 * TempLink.java
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

package playground.marcel.ptnetwork.tempelements;

import java.util.SortedMap;
import java.util.TreeMap;

import playground.marcel.ptnetwork.PtNetworkLayer;

public class TempLink{
	public TempLine line;
	public String linkID;
	public String toNodeID;
	public String fromNodeID;
	public TempHP toNode;
	public TempHP fromNode;
	public String type;
	public int cost;
	public double length;

	public TreeMap<Integer,Integer> departures = new TreeMap<Integer,Integer>();

	public TempLink(final String Id){
		this.linkID = Id;
		this.toNodeID = null;
		this.fromNodeID = null;
		this.cost = 0;
		this.type = null;
	}

	public void putTtime(final int dtime, final int ttime) {
		this.departures.put(dtime % 86400, ttime);
	}

	public long getDynTTime(final int totalSecs){

		if (this.type.equals(PtNetworkLayer.PEDESTRIAN_TYPE)) {
			return 0; // do not calc any dyn ttime for pedestrian links
		}

		int dynTTime=Integer.MIN_VALUE;

		int totSecs = totalSecs % 86400;

		if (totSecs > this.departures.lastKey().intValue()) {
			dynTTime = this.departures.get(this.departures.firstKey()).intValue();
		} else {
			SortedMap<Integer,Integer> tail = this.departures.tailMap(totSecs);
			dynTTime = tail.get(tail.firstKey()).intValue();
			System.out.println("nextdep is "+tail.firstKey());
		}

		return dynTTime;
	}

}
