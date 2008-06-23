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

import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.utils.geometry.CoordI;
import org.matsim.world.AbstractLocation;

public class Facility extends AbstractLocation {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////


	private final TreeMap<String, Activity> activities = new TreeMap<String, Activity>();
	private int numberOfVisitorsPerDay=0;

	// TODO: Set number of time bins using parameterization
	private final int numberOfTimeBins=4*24;

	/* 15 min. time bins at the moment.
	*  Every agent is at the loc. for at least 15 min. That means we have to count
	*  the arrivals and the departues using 2 variables to reduce "discretization effects".
	*/
	private int [] arrivals=null;
	private int [] departures=null;
	private int [] load=null;

	// Calculates the attractivity dependend on the shop size.
	// Will soon be replaced by more sophisticated models.
	private double attractivityFactor=1.0;

	// Calculates a penalty reflecting capcity restraints.
	// Again: will soon be replaced by more sophisticated models.
	private double capacityPenaltyFactor=1.0;


	protected Facility(final Facilities layer, final Id id, final CoordI center) {
		super(layer,id,center);
		this.arrivals = new int [this.numberOfTimeBins];
		this.departures = new int [this.numberOfTimeBins];
		this.load = new int [this.numberOfTimeBins];

		for (int i=0; i<this.numberOfTimeBins; i++){
			this.arrivals[i]=0;
			this.departures[i]=0;
			this.load[i]=0;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public double calcDistance(CoordI coord) {
		return this.center.calcDistance(coord);
	}

	public void calculateFacilityLoad24() {
		int numberOfVisitors=0;
		for (int i=0; i<this.numberOfTimeBins; i++) {
			numberOfVisitors+=this.arrivals[i];
			this.load[i]=numberOfVisitors;
			numberOfVisitors-=this.departures[i];
		}
	}

	// TODO: Remove this hardcoded parameterization asap
	public void calculateCapPenaltyFactor(int timeBinIndex) {
		//BPR
		final double a=0.8;
		final double b=8.0;
		this.capacityPenaltyFactor = a*Math.pow(this.load[timeBinIndex], b);
	}

	// TODO: Remove this hardcoded parameterization asap
	private void calculateAttractivityFactor() {

		final double a=Math.log(1.0/2500.0);

		if (this.activities.containsValue("shop_retail_lt100sqm")) {
			this.attractivityFactor = 1.0;
		}
		else if (this.activities.containsValue("shop_retail_get100sqm")) {
			this.attractivityFactor = a*Math.log(100.0);
		}
		else if (this.activities.containsValue("shop_retail_get400sqm")) {
			this.attractivityFactor = a*Math.log(400.0);
		}
		else if (this.activities.containsValue("shop_retail_get1000sqm")) {
			this.attractivityFactor = a*Math.log(1000.0);
		}
		else if (this.activities.containsValue("shop_retail_get2500sqm")) {
			this.attractivityFactor = a*Math.log(2500.0);
		}
		else if (this.activities.containsValue("shop_other")) {
			this.attractivityFactor = 1.0;
		}
		else {
			this.attractivityFactor = 1.0;
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
	public void setNumberOfVisitors(int numberOfVisitors) {
		this.numberOfVisitorsPerDay = numberOfVisitors;
	}

	public void incNumberOfVisitorsPerDay() {
		this.numberOfVisitorsPerDay++;
	}

	public void setAttractivityFactor(double attractivityFactor) {
		this.attractivityFactor = attractivityFactor;
	}

	// time in seconds from midnight
	public void addArrival(double time) {
		int timeBinIndex=Math.min(this.numberOfTimeBins-1, (int)(time/(900)));
		this.arrivals[timeBinIndex]+=1;
		this.incNumberOfVisitorsPerDay();
	}

	public void addDeparture(double time) {
		int timeBinIndex=Math.min(this.numberOfTimeBins-1, (int)(time/(900)));
		this.departures[timeBinIndex]+=1;
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

	public double getAttractivityFactor() {
		this.calculateAttractivityFactor();
		return this.attractivityFactor;
	}

	// arg: time in seconds from midnight
	public double getCapacityPenaltyFactor(double time) {
		int timeBinIndex=Math.min(this.numberOfTimeBins-1, (int)(time/(900)));
		this.calculateCapPenaltyFactor(timeBinIndex);
		return this.capacityPenaltyFactor;
	}

	// ----------------------------------------------------
	public void reset() {
		for (int i=0; i<this.numberOfTimeBins; i++) {
			this.arrivals[i]=0;
			this.departures[i]=0;
			this.load[i]=0;
		}
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
