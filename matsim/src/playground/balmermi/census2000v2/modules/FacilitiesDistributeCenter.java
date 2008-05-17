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

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;

public class FacilitiesDistributeCenter {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesDistributeCenter() {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		Gbl.random.nextDouble();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " module...");
		System.out.println("      # facilities = " + facilities.getFacilities().size());
		
		for (Facility f : facilities.getFacilities().values()) {
			CoordI c = f.getCenter();
			if (c.getX()%100 != 0) { Gbl.errorMsg("f_id="+f.getId()+" xccord is not a heactar!"); }
			if (c.getY()%100 != 0) { Gbl.errorMsg("f_id="+f.getId()+" xccord is not a heactar!"); }
			c.setX(c.getX()+Gbl.random.nextDouble()*100.0);
			c.setY(c.getY()+Gbl.random.nextDouble()*100.0);
		}

		System.out.println("      # facilities = " + facilities.getFacilities().size());
		System.out.println("    done.");
	}
}
