/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesSummary.java
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

package playground.balmermi.toggenburg.modules;

import org.apache.log4j.Logger;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;

public class FacilitiesCount {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(FacilitiesCount.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesCount() {
		log.info("init " + this.getClass().getName() + " module...");
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////

	public void run(ActivityFacilityImpl f) {
		System.out.println(f.getId());
		
	}
	
	public void run(ActivityFacilitiesImpl facilities) {
		log.info("running " + this.getClass().getName() + " module...");
		System.out.println("#facilities: "+facilities.getFacilities().size());
		log.info("done.");
	}
}
