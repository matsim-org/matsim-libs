/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractFacilityFilter.java
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

package org.matsim.facilities.filters;

import org.matsim.facilities.Facility;
import org.matsim.facilities.algorithms.FacilitiesAlgorithm;
import org.matsim.facilities.algorithms.FacilityAlgorithmI;

public abstract class AbstractFacilityFilter extends FacilitiesAlgorithm implements FacilityFilter {

	protected FacilityAlgorithmI nextAlgorithm = null;
	private int count = 0;

	public void run(Facility facility) {
		if (judge(facility)) {
			count();
			this.nextAlgorithm.run(facility);
		}
	}

	public void count() {
		this.count++;
	}

	public int getCount() {
		return this.count;
	}

}
