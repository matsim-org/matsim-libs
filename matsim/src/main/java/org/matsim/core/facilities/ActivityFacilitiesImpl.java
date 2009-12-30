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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.world.LayerImpl;
import org.matsim.world.MappedLocation;

public class ActivityFacilitiesImpl extends LayerImpl implements ActivityFacilities {

	@Deprecated
	public static final Id LAYER_TYPE = new IdImpl("facility");

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private long counter = 0;
	private long nextMsg = 1;

	private static final Logger log = Logger.getLogger(ActivityFacilitiesImpl.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	
	public ActivityFacilitiesImpl(final String name) {
		super(LAYER_TYPE, name);
	}

	/**
	 * Creates a new Facilities object with streaming switched off.
	 */
	public ActivityFacilitiesImpl() {
		this(null);
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final ActivityFacilityImpl createFacility(final Id id, final Coord center) {
		if (this.getLocations().containsKey(id)) {
			Gbl.errorMsg("Facility id=" + id + " already exists.");
		}
		ActivityFacilityImpl f = new ActivityFacilityImpl(this,id,center);
		Map<Id,MappedLocation> locations = (Map<Id, MappedLocation>) this.getLocations();
		locations.put(f.getId(),f);

		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 2;
			printFacilitiesCount();
		}

		return f;
	}
	
	public MatsimFactory getFactory() {
		throw new UnsupportedOperationException( "The factory for facilities needs to be implemented.  kai, jul09" ) ; 
	}

	@SuppressWarnings("unchecked")
	public final Map<Id, ActivityFacilityImpl> getFacilities() {
		return (Map<Id, ActivityFacilityImpl>) getLocations();
	}

	//Added 27.03.08 JH for random secondary location changes
	public final TreeMap<Id,ActivityFacilityImpl> getFacilitiesForActivityType(final String act_type) {
		TreeMap<Id,ActivityFacilityImpl> facs = new TreeMap<Id, ActivityFacilityImpl>();
		Iterator<? extends ActivityFacilityImpl> iter = this.getFacilities().values().iterator();
		while (iter.hasNext()){
			ActivityFacilityImpl f = iter.next();
			Map<String, ActivityOptionImpl> a = f.getActivityOptions();
			if(a.containsKey(act_type)){
				facs.put(f.getId(),f);
			}
		}
		return facs;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void printFacilitiesCount() {
		log.info("    facility # " + this.counter);
	}
}
