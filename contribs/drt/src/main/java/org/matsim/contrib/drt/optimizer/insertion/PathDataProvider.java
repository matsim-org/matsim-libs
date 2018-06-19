/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;

/**
 * @author michalm
 */
public interface PathDataProvider {
	class PathDataSet {
		// path[0] is a special entry; path[i] corresponds to stop i-1, for 1 <= i <= stopCount
		public final PathData[] pathsToPickup;//path[0] start->pickup
		public final PathData[] pathsFromPickup;//path[0] pickup->dropoff
		public final PathData[] pathsToDropoff;//path[0] null
		public final PathData[] pathsFromDropoff;//path[0] null

		public PathDataSet(PathData[] pathsToPickup, PathData[] pathsFromPickup, PathData[] pathsToDropoff,
				PathData[] pathsFromDropoff) {
			this.pathsToPickup = pathsToPickup;
			this.pathsFromPickup = pathsFromPickup;
			this.pathsToDropoff = pathsToDropoff;
			this.pathsFromDropoff = pathsFromDropoff;
		}
	}

	PathDataSet getPathDataSet(DrtRequest drtRequest, VehicleData.Entry vEntry);
}
