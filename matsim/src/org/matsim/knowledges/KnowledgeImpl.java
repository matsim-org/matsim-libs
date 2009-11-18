/* *********************************************************************** *
 * project: org.matsim.*
 * Knowledge.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.knowledges;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;

public class KnowledgeImpl implements Knowledge<ActivityOptionImpl> {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String desc = null;
	private static final int INIT_ACTIVITY_CAPACITY = 5;

	/**
	 * Contains all known {@link ActivityOptionImpl Activities} of a {@link PersonImpl}. Each activity can at most occur
	 * one time, independent of its {@code isPrimary} flag.
	 */
	private Set<KActivity> activities = null;
	private ArrayList<ActivitySpace> activitySpaces = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public KnowledgeImpl() {
	}

	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////

	/**
	 * Internal representation of of a pair consists of an {@link ActivityOptionImpl} and its {@code isPrimary} flag.
	 * Two {@link KActivity KActivities} are equal if the containing {@link ActivityOptionImpl Activities} are equal, independent
	 * of their {@code isPrimary} flag.
	 */
	private static class KActivity {
		/*package*/ boolean isPrimary;
		/*package*/ final ActivityOptionImpl activity;
		
		/*package*/ KActivity(ActivityOptionImpl activity, boolean isPrimary) {
			this.activity = activity;
			this.isPrimary = isPrimary;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof KActivity) {
				KActivity ka = (KActivity)obj;
				return ka.activity.equals(this.activity);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.activity.hashCode();
		}
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

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * <p>Adds an {@link ActivityOptionImpl} to a {@link PersonImpl Persons} {@link KnowledgeImpl}.
	 * It leaves the list of activities unchanged, if the given {@link ActivityOptionImpl} is already present,
	 * independent of the {@code isPrimary} flag.</p>
	 * 
	 * <p> Use {@link #setPrimaryFlag(ActivityOptionImpl, boolean)} to change the {@code isPrimary} flag of an already present {@link ActivityOptionImpl}.</p>
	 * 
	 * @param activity The {@link ActivityOptionImpl} to add to the {@link PersonImpl}s {@link KnowledgeImpl}.
	 * @param isPrimary To define if the {@code activity} is a primary activity
	 * @return <code>true</code> if the {@code activity} is not already present in the list (independent of the {@code isPrimary} flag)
	 */
	public final boolean addActivity(ActivityOptionImpl activity, boolean isPrimary) {
		if (activity == null) { return false; }
		if (activities == null) { activities = new LinkedHashSet<KActivity>(INIT_ACTIVITY_CAPACITY); }
		KActivity ka = new KActivity(activity,isPrimary);
		return activities.add(ka);
	}

	public void addActivity(ActivityOptionImpl activity) {
		this.addActivity(activity, false);
	}
	
	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Removes an {@link ActivityOptionImpl} from the {@link KnowledgeImpl} only if it is present and holds
	 * the given {@code isPrimary} flag.
	 * @param activity The {@link ActivityOptionImpl} to remove
	 * @param isPrimary The flag for which the <code>activity</code> is stored.
	 * @return <code>true</code> only if the <code>activity</code> is present and holds the given {@code isPrimary} flag.
	 */
	public final boolean removeActivity(ActivityOptionImpl activity, boolean isPrimary) {
		if (activity == null) { return false; }
		if (activities == null) { return false; }
		Iterator<KActivity> ka_it = activities.iterator();
		while (ka_it.hasNext()) {
			KActivity ka = ka_it.next();
			if (ka.activity.equals(activity) && (ka.isPrimary == isPrimary)) {
				ka_it.remove();
				if (activities.isEmpty()) { activities = null; }
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Removes an {@link ActivityOptionImpl} from the {@link KnowledgeImpl}, independent of the {@code isPrimary} flag.
	 * @param activity The {@link ActivityOptionImpl} to remove
	 * @return <code>true</code> if the <code>activity</code> was present successfully removed
	 */
	public final boolean removeActivity(ActivityOptionImpl activity) {
		if (activity == null) { return false; }
		if (activities == null) { return false; }
		boolean b = activities.remove(new KActivity(activity,false));
		if (activities.isEmpty()) { activities = null; }
		return b;
	}
	
	/**
	 * Removes all {@link ActivityOptionImpl Activities} that are equal to the given {@code isPrimary}
	 * @param isPrimary To define which of the {@link ActivityOptionImpl Activities} should be removed
	 * @return <code>true</code> only if the was at least one {@link ActivityOptionImpl} with given {@code isPrimary} to remove.
	 */
	public final boolean removeActivities(boolean isPrimary) {
		if (activities == null) { return false; }
		boolean b = false;
		Iterator<KActivity> ka_it = activities.iterator();
		while (ka_it.hasNext()) {
			KActivity ka = ka_it.next();
			if (ka.isPrimary == isPrimary) {
				ka_it.remove();
				b = true;
			}
		}
		if (activities.isEmpty()) { activities = null; }
		return b;
	}
	
	/**
	 * Removes all existing {@link ActivityOptionImpl Activities}.
	 * @return <code>true</code> if there was at least one {@link ActivityOptionImpl} given.
	 */
	public final boolean removeAllActivities() {
		if (activities == null) { return false; }
		activities.clear();
		activities = null;
		return true;
	}

	/**
	 * Removes all existing {@link ActivityOptionImpl Activities} of a given {@code act_type}
	 * @param act_type The activity type of {@link ActivityOptionImpl Activities} to remove.
	 * @return <code>true</code> if there was at least one {@link ActivityOptionImpl} of the given {@code act_type} removed.
	 */
	public final boolean removeActivities(final String act_type) {
		if (activities == null) { return false; }
		boolean b = false;
		Iterator<KActivity> ka_it = activities.iterator();
		while (ka_it.hasNext()) {
			KActivity ka = ka_it.next();
			if (ka.activity.getType().equals(act_type)) {
				ka_it.remove();
				b = true;
			}
		}
		if (activities.isEmpty()) { activities = null; }
		return b;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Returns all occurrences of an given {@link ActivityOptionImpl},
	 * that is part of a {@link ActivityFacilityImpl} with the given {@link Id}.
	 * @param facilityId The {@link Id} of a {@link ActivityFacilityImpl}
	 * @return The list of {@link ActivityOptionImpl Activities} that are part of the {@link KnowledgeImpl} and fulfill the above.
	 * The list can also be empty.
	 */
	public final ArrayList<ActivityOptionImpl> getActivities(Id facilityId) {
		if (activities == null) { return new ArrayList<ActivityOptionImpl>(0); }
		ArrayList<ActivityOptionImpl> acts = new ArrayList<ActivityOptionImpl>(INIT_ACTIVITY_CAPACITY);
		for (KActivity ka : activities) {
			if (ka.activity.getFacility().getId().equals(facilityId)) {
				acts.add(ka.activity);
			}
		}
		return acts;
	}

	/**
	 * Returns all {@link ActivityOptionImpl Activities} of the given {@code isPrimary} flag.
	 * @param isPrimary To define which of the {@link ActivityOptionImpl Activities} should be returned
	 * @return The list of {@link ActivityOptionImpl Activities} of the given flag. The list may be empty
	 */
	public final ArrayList<ActivityOptionImpl> getActivities(boolean isPrimary) {
		if (activities == null) { return new ArrayList<ActivityOptionImpl>(0); }
		ArrayList<ActivityOptionImpl> acts = new ArrayList<ActivityOptionImpl>(INIT_ACTIVITY_CAPACITY);
		for (KActivity ka : activities) {
			if (ka.isPrimary == isPrimary) {
				acts.add((ActivityOptionImpl) ka.activity);
			}
		}
		return acts;
	}
	
	/**
	 * Returns all {@link ActivityOptionImpl Activities}.
	 * @return The list of {@link ActivityOptionImpl Activities}. The list may be empty.
	 */
	public final ArrayList<ActivityOptionImpl> getActivities() {
		if (activities == null) { return new ArrayList<ActivityOptionImpl>(0); }
		ArrayList<ActivityOptionImpl> acts = new ArrayList<ActivityOptionImpl>(activities.size());
		for (KActivity ka : activities) {
			acts.add(ka.activity);
		}
		return acts;
	}

	/**
	 * Returns all {@link ActivityOptionImpl Activities} of a given activity type and a given flag
	 * @param act_type The activity type of the {@link ActivityOptionImpl Activities} should be returned
	 * @param isPrimary To define which of the {@link ActivityOptionImpl Activities} should be returned
	 * @return The list of {@link ActivityOptionImpl Activities}. The list may be empty.
	 */
	public final ArrayList<ActivityOptionImpl> getActivities(String act_type, boolean isPrimary) {
		if (activities == null) { return new ArrayList<ActivityOptionImpl>(0); }
		ArrayList<ActivityOptionImpl> acts = new ArrayList<ActivityOptionImpl>(activities.size());
		for (KActivity ka : activities) {
			if ((ka.isPrimary == isPrimary) && (ka.activity.getType().equals(act_type))) {
				acts.add(ka.activity);
			}
		}
		return acts;
	}
	
	/**
	 * Returns all {@link ActivityOptionImpl Activities} of a given activity type
	 * @param act_type The activity type of the {@link ActivityOptionImpl Activities} should be returned
	 * @return The list of {@link ActivityOptionImpl Activities}. The list may be empty.
	 */
	public final ArrayList<ActivityOptionImpl> getActivities(final String act_type) {
		if (activities == null) { return new ArrayList<ActivityOptionImpl>(0); }
		ArrayList<ActivityOptionImpl> acts = new ArrayList<ActivityOptionImpl>(activities.size());
		for (KActivity ka : activities) {
			if (ka.activity.getType().equals(act_type)) {
				acts.add(ka.activity);
			}
		}
		return acts;
	}
	
	/**
	 * Returns the set of activity types of the {@link ActivityOptionImpl Activities} with the given flag.
	 * @param isPrimary To define which of the activity types should be returned
	 * @return a set of activity types. The set may be empty.
	 */
	public final Set<String> getActivityTypes(boolean isPrimary) {
		if (activities == null) { return new TreeSet<String>(); }
		Set<String> types = new TreeSet<String>();
		for (KActivity ka : activities) {
			if (ka.isPrimary == isPrimary) {
				types.add(ka.activity.getType());
			}
		}
		return types;
	}

	/**
	 * Returns the set of activity types of the {@link ActivityOptionImpl Activities}.
	 * @return a set of activity types. The set may be empty.
	 */
	public final Set<String> getActivityTypes() {
		if (activities == null) { return new TreeSet<String>(); }
		Set<String> types = new TreeSet<String>();
		for (KActivity ka : activities) {
			types.add(ka.activity.getType());
		}
		return types;
	}
	
	
	/**
	 * Returns if a specific activity of a specific facility is primary
	 * @param act_type The activity type of the {@link ActivityOptionImpl Activities}
	 * @param facilityId The {@link Id} of a {@link ActivityFacilityImpl}
	 */
	public final boolean isPrimary(String act_type, Id facilityId) {
		if (activities == null) { 
			return false; 
		}
		for (KActivity ka : activities) {
			if ((ka.isPrimary) &&  (ka.activity.getType().equals(act_type)) &&
					(ka.activity.getFacility().getId().equals(facilityId))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns if act_type is defined as primary somewhere
	 * @param act_type The activity type of the {@link ActivityOptionImpl Activities}
	 */
	public final boolean isSomewherePrimary(String act_type) {
		if (activities == null) { 
			return false; 
		}
		for (KActivity ka : activities) {
			if ((ka.isPrimary) &&  (ka.activity.getType().equals(act_type))) {
				return true;
			}
		}
		return false;
	}
	
	

	public final String getDescription() {
		return this.desc;
	}

	/**
	 * @return List, may be null
	 */
	public final ArrayList<ActivitySpace> getActivitySpaces() {
		return this.activitySpaces;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Sets the activitySpaces to null
	 */
	public final void resetActivitySpaces(){
		this.activitySpaces = null;
	}
	
	/**
	 * Sets the {@code isPrimary} flag for the given {@link ActivityOptionImpl}
	 * @param activity the {@link ActivityOptionImpl} to set the flag
	 * @param isPrimary the flag
	 * @return <code>false</code> if the given {@link ActivityOptionImpl} does not exist.
	 */
	public final boolean setPrimaryFlag(ActivityOptionImpl activity, boolean isPrimary) {
		if (activities == null) { return false; }
		boolean found = false;
		for (KActivity ka : activities) {
			if (ka.activity.equals(activity)) {
				ka.isPrimary = isPrimary;
				found = true;
			}
		}
		return found;
	}

	/**
	 * Sets the {@code isPrimary} flag for all existing {@link ActivityOptionImpl Activities}
	 * @param isPrimary the flag
	 * @return <code>false</code> only if no {@link ActivityOptionImpl Activities} exist.
	 */
	public final boolean setPrimaryFlag(boolean isPrimary) {
		if (activities == null) { return false; }
		for (KActivity ka : activities) {
			ka.isPrimary = isPrimary;
		}
		return true;
	}
	
	public void setDescription(String desc) {
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[desc=" + this.desc + "]" + "[nof_activities=" + this.activities.size() + "]";
	}
}
