/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesSetCapacity.java
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

package playground.balmermi.census2000v2.modules;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;

public class FacilitiesRenameAndRemoveNOGAActTypes {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(FacilitiesRenameAndRemoveNOGAActTypes.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesRenameAndRemoveNOGAActTypes() {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final ActivityFacilitiesImpl facilities) {
		log.info("    running " + this.getClass().getName() + " module...");
		log.info("      # facilities = " + facilities.getFacilities().size());

		ArrayList<ActivityFacilityImpl> facs = new ArrayList<ActivityFacilityImpl>((Collection<? extends ActivityFacilityImpl>) facilities.getFacilities().values());
		facilities.getFacilities().clear();
		for (ActivityFacilityImpl f : facs) {
			throw new RuntimeException("Can't set ids anymore.");
//			ArrayList<String> types = new ArrayList<String>();
//			for (String type : f.getActivityOptions().keySet()) { types.add(type); }
//			for (int i=0; i<types.size(); i++) {
//				if (types.get(i).startsWith("B")) { f.getActivityOptions().remove(types.get(i)); }
//			}
//			ActivityFacilityImpl ff = facilities.createFacility(f.getId(),f.getCoord());
//			ff.getActivityOptions().putAll(f.getActivityOptions());
		}

		log.info("      # facilities = " + facilities.getFacilities().size());
		log.info("    done.");
	}
}
