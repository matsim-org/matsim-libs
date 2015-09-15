/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.counts;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;

public class CountStation {
	
	//private final static Logger log = Logger.getLogger(CountStation.class);
	
	private String csId;
	private LinkInfo link1;
	private LinkInfo link2;
	private Coord coord;
	private List<RawCount> counts = new Vector<RawCount>();
	
	public CountStation(String id, Coord coord) {
		this.csId = id;
		this.coord = coord;
	}
	
	public void mapCounts() {
		Iterator<RawCount> count_it = counts.iterator();
		while (count_it.hasNext()) {
			RawCount rawCount = count_it.next();	
			link1.addYearCountVal(rawCount.getHour(), rawCount.getVol1());
			link2.addYearCountVal(rawCount.getHour(), rawCount.getVol2());
			
			int date = rawCount.getDay() + 100 * rawCount.getMonth() + 10000 * rawCount.getYear();
			link1.addDailyCountVal(date, rawCount.getVol1());
			link2.addDailyCountVal(date, rawCount.getVol2());
		}
	}
	
	public boolean addSimValforLinkId(String networkName, String linkId, int hour, double simVal) {
		if (this.link1.addSimValforLinkId(networkName, linkId, hour, simVal) || 
				this.link2.addSimValforLinkId(networkName, linkId, hour, simVal)) {
			return true;
		}
		return false;
	}
	
	public void filter(TimeFilter filter) {
		this.counts = filter.filter(this.counts);
	}
	public void addCount(RawCount count) {
		this.counts.add(count);
	}		
	// aggregate 0..24
	public void aggregate(boolean removeOutliers) {
		this.link1.aggregate(removeOutliers);
		this.link2.aggregate(removeOutliers);	
	}	
	public List<RawCount> getCounts() {
		return counts;
	}
	public String getId() {
		return csId;
	}
	public void setId(String id) {
		this.csId = id;
	}
	public Coord getCoord() {
		return this.coord;
	}
	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	public LinkInfo getLink1() {
		return link1;
	}
	public void setLink1(LinkInfo link1) {
		this.link1 = link1;
	}
	public LinkInfo getLink2() {
		return link2;
	}
	public void setLink2(LinkInfo link2) {
		this.link2 = link2;
	}
	
	
/*	public void finish() {
		Collections.sort(this.counts, new CountsComparator());
	}
	
	static class CountsComparator implements Comparator<RawCount>, Serializable {
		private static final long serialVersionUID = 1L;

		public int compare(final RawCount rc0, final RawCount rc1) {
			if (rc0.getHour() < rc1.getHour()) return -1;
			else if (rc0.getHour() > rc1.getHour()) return +1;
			return 0;
		}
	}*/
}
