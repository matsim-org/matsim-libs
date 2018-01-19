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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.util.distance.DistanceUtils;

/**
 * a vehicle filtering based on beeline distances
 * 
 * @author jbischoff
 *
 */
public class DistanceFilter implements DrtVehicleFilter {

	private final double maxDistance_m_squared;

	/**
	 * @param maxDistance_m
	 *            : maximum distance, all vehicles that are further away at current time will not be taken into account
	 *            for dispatch
	 */
	public DistanceFilter(double maxDistance_m) {
		this.maxDistance_m_squared = maxDistance_m * maxDistance_m;
	}

	@Override
	public Collection<Entry> applyFilter(DrtRequest drtRequest, Collection<VehicleData.Entry> vData) {

		List<Entry> filtered = new ArrayList<>();
		for (Entry e : vData) {
			if (DistanceUtils.calculateSquaredDistance(e.start.link.getCoord(),
					drtRequest.getFromLink().getCoord()) <= maxDistance_m_squared) {
				filtered.add(e);
			}
		}
		// Logger.getLogger(getClass()).info("unfiltered: "+vData.getEntries().size()+ " filtered: "+filtered.size());
		return filtered;
	}
}
