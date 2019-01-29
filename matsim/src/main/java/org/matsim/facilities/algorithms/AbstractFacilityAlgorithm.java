/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.facilities.algorithms;

import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public abstract class AbstractFacilityAlgorithm implements FacilityAlgorithm {

	public void run(final ActivityFacilities facilities) {
		for (ActivityFacility f : facilities.getFacilities().values()) {
			run(f);
		}
	}

}
