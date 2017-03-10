/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCalcLanes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.NetworkRunnable;

/**
 * Calculates the number of lanes for each link, based on the capacity.
 */
public final class NetworkCalcLanes implements NetworkRunnable {

	@Override
	public void run(Network network) {
		double capDivider = network.getCapacityPeriod();
		for (Link link : network.getLinks().values()) {
			double capacity = link.getCapacity();
			double cap1h = capacity * 3600.0 / capDivider;

			int lanes;
			if (cap1h <= 1400) lanes = 1;
			else if (cap1h <= 3000) lanes = 2;
			else if (cap1h <= 6000) lanes = 3;
			else if (cap1h <= 8500) lanes = 4;
			else lanes = 5;
			link.setNumberOfLanes(lanes);
		}
	}
}
