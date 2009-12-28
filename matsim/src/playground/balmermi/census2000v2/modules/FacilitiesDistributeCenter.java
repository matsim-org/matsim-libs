/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesCombine.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public class FacilitiesDistributeCenter {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(FacilitiesDistributeCenter.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesDistributeCenter() {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		MatsimRandom.getRandom().nextDouble();
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final ActivityFacilitiesImpl facilities) {
		log.info("    running " + this.getClass().getName() + " module...");
		log.info("      # facilities = " + facilities.getFacilities().size());
		
		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			Coord c = f.getCoord();
			if (c.getX()%100 != 0) { Gbl.errorMsg("f_id="+f.getId()+" xccord is not a heactar!"); }
			if (c.getY()%100 != 0) { Gbl.errorMsg("f_id="+f.getId()+" xccord is not a heactar!"); }
			c.setX(c.getX()+MatsimRandom.getRandom().nextDouble()*100.0);
			c.setY(c.getY()+MatsimRandom.getRandom().nextDouble()*100.0);
		}

		log.info("      # facilities = " + facilities.getFacilities().size());
		log.info("    done.");
	}
}
