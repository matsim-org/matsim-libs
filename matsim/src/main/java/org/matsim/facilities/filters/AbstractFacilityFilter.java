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

import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.algorithms.FacilityAlgorithm;

public abstract class AbstractFacilityFilter implements FacilityAlgorithm, FacilityFilter {

	protected FacilityAlgorithm nextAlgorithm = null;
	private int count = 0;

	@Override
	public void run(final ActivityFacility facility) {
		if (judge(facility)) {
			count();
			this.nextAlgorithm.run(facility);
		}
	}

	@Override
	public void count() {
		this.count++;
	}

	@Override
	public int getCount() {
		return this.count;
	}

}
