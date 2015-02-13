/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.facilities;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

/**
 * Contains several helper methods for working with {@link ActivityFacility facilities}.
 *
 * @author cdobler
 */
public class FacilitiesUtils {
	
	private FacilitiesUtils() {} // container for static methods; do not instantiate
	
	public static ActivityFacilities createActivityFacilities() {
		return createActivityFacilities(null) ;
	}
	
	public static ActivityFacilities createActivityFacilities(String name) {
		return new ActivityFacilitiesImpl( name ) ;
	}

	/**
	 * @param network
	 * @return sorted map containing containing the facilities as values and their ids as keys.
	 */
	public static SortedMap<Id<ActivityFacility>, ActivityFacility> getSortedFacilities(final ActivityFacilities facilities) {
		return new TreeMap<>(facilities.getFacilities());
	}

}
