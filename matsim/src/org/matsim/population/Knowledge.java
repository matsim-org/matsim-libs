/* *********************************************************************** *
 * project: org.matsim.*
 * Knowledge.java
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

package org.matsim.population;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
import org.matsim.socialnetworks.mentalmap.MentalMap;
import org.matsim.socialnetworks.socialnet.EgoNet;
import org.matsim.utils.customize.CustomizableImpl;


public class Knowledge extends CustomizableImpl{

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String desc = null;

	private final ArrayList<Activity> activities = new ArrayList<Activity>();
	private ArrayList<ActivitySpace> activitySpaces = null;// = new ArrayList<ActivitySpace>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Knowledge(final String desc) {
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final ActivitySpace createActivitySpace(final String type, final String act_type) {
		ActivitySpace asp = null;
		if (type.equals("ellipse")) {
			asp = new ActivitySpaceEllipse(act_type);
		} else if (type.equals("cassini")) {
			asp = new ActivitySpaceCassini(act_type);
		}else if (type.equals("superellipse")) {
			asp = new ActivitySpaceSuperEllipse(act_type);
		}else if (type.equals("bean")) {
			asp = new ActivitySpaceBean(act_type);
		} else {
			Gbl.errorMsg("[type="+type+" not allowed]");
		}
		if (this.activitySpaces == null) {
			this.activitySpaces = new ArrayList<ActivitySpace>(1);
		}
		this.activitySpaces.add(asp);
		return asp;
	}

	public Activity getActivity(Id facilityId) {
		for (Activity a : this.activities) {
			if (a.getFacility().getId().equals(facilityId)) {
				return a;
			}
		}
		return null;
	}

	
	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	public final boolean addActivity(final Activity activity) {
		for( Activity act : this.activities ){
			if (act.getType().equals(activity.getType())) 
				if( act.getFacility().equals( activity.getFacility()))
					return false;
		}
		activities.add( activity );
		return true;
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	public final boolean removeActivity(final Activity activity) {
		return this.activities.remove(activity);
	}

	public final boolean removeActivities(final String act_type) {
		boolean removed = false;
		for( Activity act : activities.toArray(new Activity[0])){
			if (act.getType().equals(act_type)){ 
				activities.remove( act );
				removed = true;
			}
		}
		return removed;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public final ArrayList<Activity> getActivities() {
		return this.activities;
	}

	public final ArrayList<Activity> getActivities(final String act_type) {
		ArrayList<Activity> acts = new ArrayList<Activity>();
		for (int i=0; i<this.activities.size(); i++) {
			Activity a = this.activities.get(i);
			if (a.getType().equals(act_type)) { acts.add(a); }
		}
		return acts;
	}

	public final TreeSet<String> getActivityTypes() {
		TreeSet<String> act_types = new TreeSet<String>();
		for (int i=0; i<this.activities.size(); i++) {
			act_types.add(this.activities.get(i).getType());
		}
		return act_types;
	}

	public final String getDesc() {
		return this.desc;
	}
	/**
	 * 
	 * @return List, may be null
	 */
	public final ArrayList<ActivitySpace> getActivitySpaces() {
		return this.activitySpaces;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[desc=" + this.desc + "]" + "[nof_activities=" + this.activities.size() + "]";
	}
	
	//////////////////////////////////////////////////////////////////////
	// for socialnetworks package
	//////////////////////////////////////////////////////////////////////

	private MentalMap map = null;

	public void setMentalMap(MentalMap map) {
		this.map = map;
	}

	public MentalMap getMentalMap() {
		if (this.map == null) { this.map = new MentalMap(this); }
		return map;
	}

	private EgoNet egoNet = null;

	public void setEgoNet(EgoNet egoNet) {
		this.egoNet = egoNet;
	}

	public EgoNet getEgoNet() {
		if (this.egoNet == null) { this.egoNet = new EgoNet(); }
		return egoNet;
	}
}
