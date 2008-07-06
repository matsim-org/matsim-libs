/* *********************************************************************** *
 * project: org.matsim.*
 * Facility.java
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

package org.matsim.facilities;

import java.util.Iterator;
import java.util.TreeMap;

//import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.utils.geometry.CoordI;
import org.matsim.world.AbstractLocation;

public class Facility extends AbstractLocation {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	/*
	 * TODO:
	 * 1.	At the moment a facility has activities and an activity has a capacity.
	 * 		We have to define this more precise:
	 * 		For work and home in the same facility we need two independent capacities
	 * 		(-> could be done in Activity)
	 * 		But shopping and leisure in a shopping mall with cinemas has to be treated with one cap
	 * 		(-> so it is better handled in Facility)
	 *
	 * 		At the moment I need only shopping (and leisure) thus I only use one cap.
	 * 		(The smallest of all shopping (and leisure) activities of the facility).
	 *
	 * 2.	The mobsim handles times > 24 h
	 *		Facility load has to be handled for hour 0..24 only (acc. to M.B.)
	 */
	//private final static Logger log = Logger.getLogger(Facility.class);

	private final TreeMap<String, Activity> activities = new TreeMap<String, Activity>();
	private int numberOfVisitorsPerDay = 0;

	// TODO: Set number of time bins using parameterization
	private final int numberOfTimeBins = 4*24;

	// for the moment use just one capacity TODO: See point 1. in the remark on top.
	private int capacity = 0;

	/* 15 min. time bins at the moment.
	*  Every agent is at the loc. for at least 15 min. That means we have to count
	*  the arrivals and the departures using 2 variables to reduce "discr. effects".
	*/
	private int [] arrivals = null;
	private int [] departures = null;
	private int [] load = null;

	// Calculates the attractiveness dependent on the shop size.
	// Will soon be replaced by more sophisticated models.
	private double attrFactor = 1.0;

	private double sumCapacityPenaltyFactor = 0.0;


	protected Facility(final Facilities layer, final Id id, final CoordI center) {
		super(layer,id,center);
		this.arrivals = new int [this.numberOfTimeBins];
		this.departures = new int [this.numberOfTimeBins];
		this.load = new int [this.numberOfTimeBins];

		for (int i=0; i<this.numberOfTimeBins; i++){
			this.arrivals[i] = 0;
			this.departures[i] = 0;
			this.load[i] = 0;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public double calcDistance(CoordI coord) {
		return this.center.calcDistance(coord);
	}

	public void calculateFacilityLoad24(int scaleNumberOfPersons) {
		int numberOfVisitors = 0;
		for (int i=0; i<this.numberOfTimeBins; i++) {
			numberOfVisitors += this.arrivals[i];
			this.load[i] = numberOfVisitors*scaleNumberOfPersons;
			numberOfVisitors -= this.departures[i];
		}
	}

	// TODO: Remove this hard-coded parameterization asap
	public double calculateCapPenaltyFactor(int startTimeBinIndex, int endTimeBinIndex) {

		double capPenaltyFactor = 0.0;
		if (this.capacity > 0) {
			//BPR
			final double a=0.8;
			final double b=8.0;
			for (int i=startTimeBinIndex; i<endTimeBinIndex+1; i++) {
				capPenaltyFactor += a*Math.pow(
						(double)this.load[i]/((double)this.capacity/(double)this.numberOfTimeBins), b);
			}

			capPenaltyFactor /= (endTimeBinIndex-startTimeBinIndex+1);
			capPenaltyFactor = Math.min(1.0, capPenaltyFactor);
		}
		else {
			capPenaltyFactor = 1.0;
		}
		this.sumCapacityPenaltyFactor += capPenaltyFactor;
		return capPenaltyFactor;
	}

	// TODO: Remove this hard-coded parameterization asap
	private void calculateAttrFactor() {

		final double a=1.0/Math.log(2500.0);

		if (this.activities.containsKey("shop_retail_lt100sqm")) {
			this.attrFactor = 1.0;
		}
		else if (this.activities.containsKey("shop_retail_get100sqm")) {
			this.attrFactor = 1.0+a*Math.log(100.0);
		}
		else if (this.activities.containsKey("shop_retail_get400sqm")) {
			this.attrFactor = 1.0+a*Math.log(400.0);
		}
		else if (this.activities.containsKey("shop_retail_get1000sqm")) {
			this.attrFactor = 1.0+a*Math.log(1000.0);
		}
		else if (this.activities.containsKey("shop_retail_get2500sqm")) {
			this.attrFactor = 1.0+a*Math.log(2500.0);
		}
		else if (this.activities.containsKey("shop_other")) {
			this.attrFactor = 1.0;
		}
		else {
			this.attrFactor = 1.0;
		}
	}


	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Activity createActivity(final String type) {
		if (this.activities.containsKey(type)) {
			Gbl.errorMsg(this + "[type=" + type + " already exists]");
		}
		String type2 = type.intern();
		Activity a = new Activity(type2, this);
		this.activities.put(type2, a);
		return a;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////
	public void setNumberOfVisitorsPerDay(int numberOfVisitorsPerDay) {
		this.numberOfVisitorsPerDay = numberOfVisitorsPerDay;
	}

	public void addVisitorsPerDay(int scaleNumberOfPersons) {
		this.numberOfVisitorsPerDay += scaleNumberOfPersons;
	}

	public void setAttrFactor(double attrFactor) {
		this.attrFactor = attrFactor;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	// time in seconds from midnight
	public void addArrival(double time, int scaleNumberOfPersons) {
		// we do not handle times > 24h
		// we do not care about #arrivals==#departures after the last time bin
		if (time > 24.0*3600.0) {
			return;
		}
		int timeBinIndex=Math.min(this.numberOfTimeBins-1, (int)(time/(900)));
		this.arrivals[timeBinIndex]+=1;
		this.addVisitorsPerDay(scaleNumberOfPersons);
	}

	public void addDeparture(double time) {
		// we do not handle times > 24h
		// we do not care about #arrivals==#departures after the last time bin
		if (time > 24.0*3600.0) {
			return;
		}
		int timeBinIndex=Math.min(this.numberOfTimeBins-1, (int)(time/(900)));
		this.departures[timeBinIndex]+=1;
	}

	public void setSumCapacityPenaltyFactor(double sumCapacityPenaltyFactor) {
		this.sumCapacityPenaltyFactor = sumCapacityPenaltyFactor;
	}
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final TreeMap<String,Activity> getActivities() {
		return this.activities;
	}

	public final Activity getActivity(final String type) {
		return this.activities.get(type);
	}

	public final Link getLink() {
		if (this.down_mapping.isEmpty()) { return null; }
		if (this.down_mapping.size() > 1) { Gbl.errorMsg("Something is wrong!!! A facility contains at most one Link (as specified for the moment)!"); }
		return (Link)this.getDownMapping().get(this.down_mapping.firstKey());
	}

	public int getNumberOfVisitorsPerDay() {
		return this.numberOfVisitorsPerDay;
	}

	public double getAttrFactor() {
		this.calculateAttrFactor();
		return this.attrFactor;
	}

	// arg: time in seconds from midnight
	public double getCapacityPenaltyFactor(double startTime, double endTime) {

		if (startTime>24.0*3600.0 && endTime>24.0*3600.0) {
			return 0.0;
		}
		else if (endTime>24.0*3600.0) {
			endTime=24.0*3600.0;
		}

		int startTimeBinIndex = Math.min(this.numberOfTimeBins-1, (int)(startTime/(900)));
		int endTimeBinIndex = Math.min(this.numberOfTimeBins-1, (int)(endTime/(900)));
		return calculateCapPenaltyFactor(startTimeBinIndex, endTimeBinIndex);
	}

	// We do not have shopping and leisure acts in ONE facility
	// Give a constant cap at the moment, cap from facilitiesV3 are not useful.
	// peak cap = 10 % of day cap
	private int getCapacityForShoppingAndLeisure() {
		int cap = Integer.MAX_VALUE;

		Iterator<Activity> act_it=this.activities.values().iterator();
		while (act_it.hasNext()){
			Activity activity = act_it.next();
			if (activity.getType().startsWith("s")) {
				cap = 234;
				/*
				if (activity.getCapacity() < cap) {
					cap = activity.getCapacity();
				}
				*/
			}
			if (activity.getType().startsWith("l")) {
				cap = 615;
			}
		}
		return Math.max(10 , cap);
	}

	public int getCapacity() {
		return this.capacity;
	}
	
	public double getSumCapacityPenaltyFactor() {
		return sumCapacityPenaltyFactor;
	}

	// ----------------------------------------------------
	public void reset() {
		for (int i=0; i<this.numberOfTimeBins; i++) {
			this.arrivals[i] = 0;
			this.departures[i] = 0;
			this.load[i] = 0;
			this.numberOfVisitorsPerDay = 0;
			this.sumCapacityPenaltyFactor = 0.0;
		}
	}

	public void finish() {
		this.capacity = this.getCapacityForShoppingAndLeisure();
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString() +
		       "[nof_activities=" + this.activities.size() + "]";
	}

	

}
