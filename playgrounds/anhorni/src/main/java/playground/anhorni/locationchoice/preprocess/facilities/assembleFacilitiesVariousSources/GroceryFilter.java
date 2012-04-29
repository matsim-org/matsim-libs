/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class GroceryFilter {
	
	String [] types2remove = {"Restaurant", "Parfumerie", "Interdiscount", "Bau und Hobby", "Jelmoli Fundgrube"};
	
	public List<ZHFacilityComposed> filterFacilities(List<ZHFacilityComposed> facilities) {
		
		Vector<ZHFacilityComposed> filteredFacilities = new Vector<ZHFacilityComposed>(); 
		
		Iterator<ZHFacilityComposed> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed facility = facilities_it.next();
			
			boolean set = true;
			for (int i = 0; i < types2remove.length; i++) {
				if (facility.getName().contains(types2remove[i])) {
					set = false;
					continue;
				}
			}	
			if (set) {
				filteredFacilities.add(facility);
			}
		}
		return filteredFacilities;
	}
	
}
