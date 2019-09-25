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
package playground.vsp.analysis.modules.ptRoutes2paxAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author droeder
 *
 */
public class TransitRouteContainer {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitRouteContainer.class);
	private Double interval;
	private Id id;
	private Integer maxSlice;
	private Counts boarding;
	private Counts alighting;
	private Counts capacity;
	private Counts totalPax;
	private Counts occupancy;

	/**
	 * The number of Pax boarding/alighting at different stopFacilities (served by the route) is counted per 
	 * interval/timeslice. Furthermore the number of "inVehiclePeople" when vehicles depart and the capacity 
	 * of departing vehicles is evaluated.
	 * 
	 * Assuming the countsinterval is 3,600s
	 * 
	 * @param r
	 * @param countsInterval, the length of an interval/timeslice ins second
	 * @param maxSlice, the maximum number of timeslices. In the last timeslice all numbers of later e.g. boardings are summed up...
	 */
	public TransitRouteContainer(TransitRoute r, double countsInterval, int maxSlice) {
		this.id = r.getId();
		this.boarding = new Counts();
		this.alighting = new Counts();
		this.capacity = new Counts();
		this.totalPax = new Counts();
		this.occupancy = new Counts();
		this.interval = countsInterval;
		this.maxSlice = maxSlice;
		for(int i= 0; i < r.getStops().size() ; i++){
			TransitRouteStop s = r.getStops().get(i);
			create(this.boarding, Id.create(i, Link.class), s.getStopFacility().getId(), 0., 0.);
			create(this.alighting, Id.create(i, Link.class), s.getStopFacility().getId(), 0., 0.);
			create(this.capacity, Id.create(i, Link.class), s.getStopFacility().getId(), 0., 0.);
			create(this.totalPax, Id.create(i, Link.class), s.getStopFacility().getId(), 0., 0.);
			create(this.occupancy, Id.create(i, Link.class), s.getStopFacility().getId(), 0., 0.);
		}
	}
	
	public Id getId(){
		return this.id;
	}

	/**
	 * @param facilityId
	 * @param time
	 */
	public void paxBoarding(Id<Link> stopIndexId, double time) {
		increase(this.boarding, stopIndexId, time, 1.);
	}

	/**
	 * @param facilityId
	 * @param time
	 */
	public void paxAlighting(Id<Link> stopIndexId, double time) {
		increase(this.alighting, stopIndexId, time, 1.);
	}

	/**
	 * @param time
	 * @param vehCapacity
	 * @param nrSeatsInUse
	 * @param stopIndexId
	 */
	public void vehicleDeparts(double time, double vehCapacity,	double nrSeatsInUse, Id<Link> stopIndexId) {
		if(this.alighting.getCount(stopIndexId).getVolume(getTimeSlice(time)) == null){
			set(this.alighting, stopIndexId, time, 0);
		}
		if(this.boarding.getCount(stopIndexId).getVolume(getTimeSlice(time)) == null){
			set(this.boarding, stopIndexId, time, 0);
		}
		increase(this.capacity, stopIndexId, time, vehCapacity);
		increase(this.totalPax, stopIndexId, time, nrSeatsInUse);
		Integer slice = getTimeSlice(time);
		set(this.occupancy, stopIndexId, time, this.totalPax.getCount(stopIndexId).getVolume(slice).getValue() /
				this.capacity.getCount(stopIndexId).getVolume(slice).getValue());
	}
	
	private void increase(Counts counts, Id<Link> stopId, Double time, double increaseBy){
		//create a new count
		Count count = counts.createAndAddCount(stopId, stopId.toString());
		if(count == null){
			//or get the old one if there is one
			count = counts.getCount(stopId);
		}
		Integer slice = getTimeSlice(time);
//		if(slice > this.maxSlice){
//			this.maxSlice = slice;
//		}
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(v.getValue() + increaseBy);
	}
	
	private void set(Counts counts, Id<Link> stopIndexId, Double time, double value){
		//create a new count
		Count count = counts.createAndAddCount(stopIndexId, stopIndexId.toString());
		if(count == null){
			//or get the old one if there is one
			count = counts.getCount(stopIndexId);
		}
		Integer slice = getTimeSlice(time);
//		if(slice > this.maxSlice){
//			this.maxSlice = slice;
//		}
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(value);
	}
	
	private void create(Counts counts, Id<Link> stopIndexId, Id<org.matsim.facilities.Facility> stationName, Double time, double value){
		//create a new count
		Count count = counts.createAndAddCount(stopIndexId, stationName.toString());
//		if(count == null){
//			//or get the old one if there is one
//			count = counts.getValue(stopIndexId);
//		}
//		Integer slice = getTimeSlice(time);
////		if(slice > this.maxSlice){
////			this.maxSlice = slice;
////		}
//		Volume v;
//		if(count.getVolumes().containsKey(slice)){
//			v = count.getVolume(slice);
//		}else{
//			v = count.createVolume(slice, 0);
//		}
//		v.setValue(value);
	}
	
	private Integer getTimeSlice(double time){
		int slice =  (int) (time / this.interval) + 1;
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

