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

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.util.PartialSort;
import org.matsim.contrib.util.distance.DistanceUtils;

/**
 * filters out the k nearest vehicles to a request.
 *
 * @author jbischoff
 */
public class KNearestVehicleFilter implements DrtVehicleFilter {
	private final int k;

	public KNearestVehicleFilter(int k) {
		this.k = k;
	}

	@Override
	public Collection<Entry> applyFilter(DrtRequest drtRequest, Collection<VehicleData.Entry> vData) {
		Link toLink = drtRequest.getFromLink();
		PartialSort<VehicleData.Entry> nearestVehicleSort = new PartialSort<VehicleData.Entry>(k);

		for (VehicleData.Entry veh : vData) {
			double squaredDistance = DistanceUtils.calculateSquaredDistance(veh.start.link, toLink);
			nearestVehicleSort.add(veh, squaredDistance);
		}

		return nearestVehicleSort.retriveKSmallestElements();
	}
}
