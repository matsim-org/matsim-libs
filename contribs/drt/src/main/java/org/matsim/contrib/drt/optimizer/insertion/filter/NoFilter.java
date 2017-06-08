/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.drt.optimizer.insertion.filter;

import java.util.List;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;

/**
 * @author  jbischoff
 *
 */
/**
 * A pseudo filter, returning the whole set.
 */
public class NoFilter implements DrtVehicleFilter {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.optimizer.insertion.filter.DrtVehicleFilter#applyFilter(org.matsim.contrib.drt.data.DrtRequest, org.matsim.contrib.drt.optimizer.VehicleData)
	 */
	@Override
	public List<Entry> applyFilter(DrtRequest drtRequest, VehicleData vData) {

		return vData.getEntries();
	}

}
