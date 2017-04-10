/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer;

import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.contrib.util.*;

/**
 * kNN - k Nearest Neighbours
 *
 * @author michalm
 */
public class StraightLineKnnFinders {
	public static <D> StraightLineKnnFinder<VehicleData.Entry, DestEntry<D>> createDestEntryFinder(int k) {
		if (k < 0) {
			return null;
		}

		LinkProvider<DestEntry<D>> linkProvider = LinkProviders.createDestEntryToLink();
		return new StraightLineKnnFinder<>(k, LinkProviders.VEHICLE_ENTRY_TO_LINK, linkProvider);
	}

	public static <D> StraightLineKnnFinder<DestEntry<D>, VehicleData.Entry> createVehicleDepartureFinder(int k) {
		if (k < 0) {
			return null;
		}

		LinkProvider<DestEntry<D>> linkProvider = LinkProviders.createDestEntryToLink();
		return new StraightLineKnnFinder<>(k, linkProvider, LinkProviders.VEHICLE_ENTRY_TO_LINK);
	}
}
