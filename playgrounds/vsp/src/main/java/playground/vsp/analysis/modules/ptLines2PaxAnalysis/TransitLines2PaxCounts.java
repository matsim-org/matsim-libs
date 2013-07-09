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

package playground.vsp.analysis.modules.ptLines2PaxAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * @author sfuerbas (parts taken from droeder)
 *
 */

public class TransitLines2PaxCounts {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitLines2PaxCounts.class);
	private Double interval;
	private Id id;
	private Integer maxSlice;
	private Counts boarding;
	private Counts alighting;
	private Counts capacity;
	private Counts totalPax;
	private Counts occupancy;
	private List<TransitRouteStop> longestRoute_a;
	private List<TransitRouteStop> longestRoute_b;
	private List<TransitRoute> routeList;
	
	public TransitLines2PaxCounts (TransitLine tl, double countsInterval, int maxSlice) {
		this.id = tl.getId();
		this.boarding = new Counts();
		this.alighting = new Counts();
		this.capacity = new Counts();
		this.totalPax = new Counts();
		this.occupancy = new Counts();
		this.interval = countsInterval;
		this.maxSlice = maxSlice;
		this.routeList = new ArrayList<TransitRoute>();
		for (TransitRoute tr : tl.getRoutes().values()) {
//			findLongestRoute(tr.getStops());
			this.routeList.add(tr);
			int numberOfStops = tr.getStops().size();
			for (int ii=0; ii < numberOfStops; ii++) {
				TransitRouteStop s = tr.getStops().get(ii);
				Id stopFacilId = s.getStopFacility().getId();
				if (this.boarding.getCounts().get(stopFacilId)==null) {
					this.boarding.createCount(stopFacilId, stopFacilId.toString());
					this.alighting.createCount(stopFacilId, stopFacilId.toString());
					this.capacity.createCount(stopFacilId, stopFacilId.toString());
					this.totalPax.createCount(stopFacilId, stopFacilId.toString());
					this.occupancy.createCount(stopFacilId, stopFacilId.toString());
				}
			}
		}	
		
	}
	
//	not sure this works properly
	
	public List<TransitRoute> getRoutesByNumberOfStops() {
		Collections.sort(this.routeList, new RouteSizeComparator());
		return this.routeList;
	}
	
	public class RouteSizeComparator implements Comparator<TransitRoute> {
		public int compare(TransitRoute route1, TransitRoute route2) {
			Integer stopSize1 = route1.getStops().size();
			Integer stopSize2 = route2.getStops().size();
			if (stopSize1 > stopSize2) return 1;
			else if (stopSize1 > stopSize2) return -1;
			else return 0;
		}
	}
	
	@SuppressWarnings("unused")
	private void findLongestRoute (List<TransitRouteStop> route_c) {
		int length_a;
		int length_b;
		int length_c = route_c.size();
		if (this.longestRoute_a == null && this.longestRoute_b == null) {
			this.longestRoute_a = route_c;
		}
		else if (this.longestRoute_a != null && this.longestRoute_b == null) {
			length_a = this.longestRoute_a.size();
			if (length_c > length_a && route_c.containsAll(this.longestRoute_a)) {
				this.longestRoute_a = route_c;
			}
			else if (!this.longestRoute_a.containsAll(route_c) && !route_c.containsAll(this.longestRoute_a)) {
				this.longestRoute_b = route_c;
			}
		}
		else if (this.longestRoute_a != null && this.longestRoute_b != null) {
			length_a = this.longestRoute_a.size();
			length_b = this.longestRoute_b.size();
			if (length_c > length_a && route_c.containsAll(this.longestRoute_a)) {
				this.longestRoute_a = route_c;
			}
			else if (length_c > length_b && route_c.containsAll(longestRoute_b)) {
				this.longestRoute_b = route_c;
			}
		}
	}
	
	
	public List<TransitRouteStop> getLongestRoute_a() {
		return longestRoute_a;
	}

	public List<TransitRouteStop> getLongestRoute_b() {
		return longestRoute_b;
	}

	public Id getId(){
		return this.id;
	}

	/**
	 * @param facilityId
	 * @param time
	 */
	public void paxBoarding(Id facilityId, double time) {
		increase(this.boarding, facilityId, time, 1.);
	}

	/**
	 * @param facilityId
	 * @param time
	 */
	public void paxAlighting(Id facilityId, double time) {
		increase(this.alighting, facilityId, time, 1.);
	}

	/**
	 * @param time
	 * @param vehCapacity
	 * @param nrSeatsInUse
	 * @param stopIndexId
	 */
	public void vehicleDeparts(double time, double vehCapacity,	double nrSeatsInUse, Id stopFacilityId) {
		if(this.alighting.getCount(stopFacilityId).getVolume(getTimeSlice(time)) == null){
			set(this.alighting, stopFacilityId, time, 0);
		}
		if(this.boarding.getCount(stopFacilityId).getVolume(getTimeSlice(time)) == null){
			set(this.boarding, stopFacilityId, time, 0);
		}
		increase(this.capacity, stopFacilityId, time, vehCapacity);
		increase(this.totalPax, stopFacilityId, time, nrSeatsInUse);
		Integer slice = getTimeSlice(time);
		set(this.occupancy, stopFacilityId, time, this.totalPax.getCount(stopFacilityId).getVolume(slice).getValue() /
				this.capacity.getCount(stopFacilityId).getVolume(slice).getValue());
	}
	
	private void increase(Counts counts, Id stopFacilityId, Double time, double increaseBy){
		Count count = counts.getCount(stopFacilityId);
		Integer slice = getTimeSlice(time);
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(v.getValue() + increaseBy);
	}
	
	private void set(Counts counts, Id stopFacilityId, Double time, double value){
		Count count =  counts.getCount(stopFacilityId);
		Integer slice = getTimeSlice(time);
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(value);
	}
	
	private Integer getTimeSlice(double time){
		int slice =  (int) (time / this.interval);
		if(slice >= this.maxSlice){
			return this.maxSlice;
		}
		return slice;
	}

	/**
	 * @return the boarding
	 */
	public Counts getBoarding() {
		return boarding;
	}

	/**
	 * @return the alighting
	 */
	public Counts getAlighting() {
		return alighting;
	}

	/**
	 * @return the capacity
	 */
	public Counts getCapacity() {
		return capacity;
	}

	/**
	 * @return the totalPax
	 */
	public Counts getTotalPax() {
		return totalPax;
	}

	/**
	 * @return the occupancy
	 */
	public Counts getOccupancy() {
		return occupancy;
	}
	
	public Integer getMaxSlice(){
		return this.maxSlice;
	}

}
