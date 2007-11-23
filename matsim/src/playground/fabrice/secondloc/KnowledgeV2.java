/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeV2.java
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

package playground.fabrice.secondloc;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.plans.ActivitySpace;
import org.matsim.plans.ActivitySpaceBean;
import org.matsim.plans.ActivitySpaceCassini;
import org.matsim.plans.ActivitySpaceEllipse;
import org.matsim.plans.ActivitySpaceSuperEllipse;

public class KnowledgeV2 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unused")
	private static final int DEBUG_LEVEL = 1;

	private String desc = null;

	// TreeMap(String type, ActivityFacilities activityfacilities)
	private final TreeMap<String,ActivityFacilities> activityfacilities = new TreeMap<String,ActivityFacilities>();
	// TreeMap(String act_type, ActivitySpace act_space)
	private final ArrayList<ActivitySpace> act_spaces = new ArrayList<ActivitySpace>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	protected KnowledgeV2(final String desc) {
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final ActivityFacilities createActivityFacility(final String act_type) {
		ActivityFacilities af = this.activityfacilities.get(act_type);
		if (af == null) {
			af = new ActivityFacilities(act_type);
			this.activityfacilities.put(act_type,af);
		}
		return af;
	}

	public final ActivitySpace createActivitySpace(final String type, final String act_type) {
//		if (this.act_spaces.containsKey(act_type)) {
//			Gbl.errorMsg(this.getClass(),"createActivitySpace(...)","[act_type="+act_type+" is already defined]");
//		}
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
//		this.act_spaces.put(act_type,asp);
		this.act_spaces.add(asp);
		return asp;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getDesc() {
		return this.desc;
	}

	public final TreeMap<String,ActivityFacilities> getActivityFacilities() {
		return this.activityfacilities;
	}

	public final ArrayList<ActivitySpace> getActivitySpaces() {
		return this.act_spaces;
	}
	
	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final String toString() {
		return "[desc=" + this.desc + "]" +
				"[nof_activityfacilities=" + this.activityfacilities.size() + "]";
	}
	
	//
	// The methods and fields below are needed only by the secondary 
	// location choice so far. fabrice
	// Copy this class to org/matsim/demandmodelling/plans/Knowledge.java
	
//	public KnowledgeHacks hacks = new KnowledgeHacks( this );
//	public MentalMap map = new MentalMap();
//	public SocialContext context = new SocialContext();
}
