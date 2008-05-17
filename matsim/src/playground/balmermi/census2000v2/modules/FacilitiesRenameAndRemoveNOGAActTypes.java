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

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.algorithms.FacilitiesAlgorithm;

public class FacilitiesRenameAndRemoveNOGAActTypes extends FacilitiesAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesRenameAndRemoveNOGAActTypes() {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " module...");
		System.out.println("      # facilities = " + facilities.getFacilities().size());

		ArrayList<Facility> facs = new ArrayList<Facility>(facilities.getFacilities().values());
		facilities.getFacilities().clear();
		for (Facility f : facs) {
			f.setId(new IdImpl(Integer.parseInt(f.getId().toString())+10000000));
			ArrayList<String> types = new ArrayList<String>();
			for (String type : f.getActivities().keySet()) { types.add(type); }
			for (int i=0; i<types.size(); i++) {
				if (types.get(i).startsWith("B")) { f.getActivities().remove(types.get(i)); }
			}
			Facility ff = facilities.createFacility(f.getId(),f.getCenter());
			ff.getActivities().putAll(f.getActivities());
		}
		
		System.out.println("      # facilities = " + facilities.getFacilities().size());
		System.out.println("    done.");
	}
}
