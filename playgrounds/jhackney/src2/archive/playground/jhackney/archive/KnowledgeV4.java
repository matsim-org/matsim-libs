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

package playground.jhackney.mentalmap;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.ActivitySpace;
import org.matsim.population.ActivitySpaceBean;
import org.matsim.population.ActivitySpaceCassini;
import org.matsim.population.ActivitySpaceEllipse;
import org.matsim.population.ActivitySpaceSuperEllipse;

import playground.jhackney.socialnet.EgoNet;
import playground.jhackney.mentalmap.MentalMap;

public class Knowledge {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String desc = null;

	private final ArrayList<Activity> activities = new ArrayList<Activity>();
	private final TreeMap<Id,Facility> facilities = new TreeMap<Id,Facility>();
	private final ArrayList<ActivitySpace> act_spaces = new ArrayList<ActivitySpace>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	protected Knowledge(final String desc) {
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
		this.act_spaces.add(asp);
		return asp;
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	public final boolean addActivity(final Activity activity) {
		if (!this.activities.contains(activity)) { this.activities.add(activity); }
		Facility f = activity.getFacility();
		this.facilities.put(f.getId(), f);
		return true;
	}
	
	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	public final boolean removeActivity(final Activity activity) {
		boolean removed = this.activities.remove(activity);
		if (removed) {
			Facility f = activity.getFacility();
			for (int i=0; i<this.activities.size(); i++) {
				Facility other = this.activities.get(i).getFacility();
				if (other.equals(f)) { return true; }
			}
			this.facilities.remove(f.getId());
			return true;
		}
		return false;
	}
	
	public final boolean removeActivites(final String act_type) {
		boolean removed = false;
		for (int i=0; i<this.activities.size(); i++) {
			Activity a = this.activities.get(i);
			if (a.getType().equals(act_type)) {
				boolean b = this.removeActivity(a);
				if (b) { removed = true; }
			}
		}
		return removed;
	}
	
	public final boolean removeFacility(final Facility facility) {
		Facility f_removed = this.facilities.remove(facility.getId());
		if (f_removed == null) { return false; }
		if (f_removed != facility) { Gbl.errorMsg("Something is completely wrong!"); }
		ArrayList<Activity> to_be_removed = new ArrayList<Activity>();
		for (int i=0; i<this.activities.size(); i++) {
			Activity a = this.activities.get(i);
			if (a.getFacility() == f_removed) { to_be_removed.add(a); }
		}
		return this.activities.removeAll(to_be_removed);
	}
	
	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setDesc(String desc) {
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	public final boolean containsFacility(final Id id) {
		return this.facilities.containsKey(id);
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final TreeMap<Id,Facility> getFacilities() {
		return this.facilities;
	}
	
	public final TreeMap<Id,Facility> getFacilities(final String act_type) {
		TreeMap<Id,Facility> facs = new TreeMap<Id, Facility>();
		for (int i=0; i<this.activities.size(); i++) {
			Activity a = this.activities.get(i);
			Facility f = a.getFacility();
			if (a.getType().equals(act_type)) { facs.put(f.getId(),f); }
		}
		return facs;
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

	public final ArrayList<ActivitySpace> getActivitySpaces() {
		return this.act_spaces;
	}
	
	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[desc=" + this.desc + "]" + "[nof_activities=" + this.activities.size() + "]";
	}

	//////////////////////////////////////////////////////////////////////
	// social network methods
	//////////////////////////////////////////////////////////////////////

	//public KnowledgeHacks hacks = new KnowledgeHacks( this );
	public MentalMap map = new MentalMap(this);
	public EgoNet egoNet = new EgoNet();
}
