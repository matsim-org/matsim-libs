/* *********************************************************************** *
 * project: org.matsim.*
 * Facilities.java
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

package org.matsim.core.facilities;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.BasicLocations;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.gbl.Gbl;

public class ActivityFacilitiesImpl implements ActivityFacilities, BasicLocations {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private long counter = 0;
	private long nextMsg = 1;

	private static final Logger log = Logger.getLogger(ActivityFacilitiesImpl.class);

	private Map<Id, ActivityFacility> facilities = new TreeMap<Id, ActivityFacility>();

	private String name;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public ActivityFacilitiesImpl(final String name) {
		this.name = name;
	}

	public ActivityFacilitiesImpl() {
		this(null);
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final ActivityFacilityImpl createFacility(final Id id, final Coord center) {
		if (facilities.containsKey(id)) {
			Gbl.errorMsg("Facility id=" + id + " already exists.");
		}
		ActivityFacilityImpl f = new ActivityFacilityImpl(id,center,null);
		facilities.put(f.getId(),f);

		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 2;
			printFacilitiesCount();
		}

		return f;
	}

	@Override
	public MatsimFactory getFactory() {
		throw new UnsupportedOperationException( "The factory for facilities needs to be implemented.  kai, jul09" ) ;
	}

	@Override
	public final Map<Id, ActivityFacility> getFacilities() {
		return facilities;
	}

	//Added 27.03.08 JH for random secondary location changes
	public final TreeMap<Id, ActivityFacility> getFacilitiesForActivityType(final String act_type) {
		TreeMap<Id,ActivityFacility> facs = new TreeMap<Id, ActivityFacility>();
		Iterator<ActivityFacility> iter = this.facilities.values().iterator();
		while (iter.hasNext()){
			ActivityFacility f = iter.next();
			Map<String, ? extends ActivityOption> a = f.getActivityOptions();
			if(a.containsKey(act_type)){
				facs.put(f.getId(),f);
			}
		}
		return facs;
	}

	public final void printFacilitiesCount() {
		log.info("    facility # " + this.counter);
	}

	@Override
	public BasicLocation getLocation(Id locationId) {
		return getFacilities().get(locationId);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
