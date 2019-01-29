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

package org.matsim.contrib.drt.optimizer.rebalancing;

import java.util.List;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;

/**
 * Idle vehicles (=StayTask) may be re-allocated using this interface.
 * 
 * @author michalm
 */
public interface RebalancingStrategy {
	public class Relocation {
		public final Vehicle vehicle;
		public final Link link;

		public Relocation(Vehicle vehicle, Link link) {
			this.vehicle = vehicle;
			this.link = link;
		}
	}

	/**
	 * This method is called at each re-balancing step (interval defined in config).
	 * 
	 */
	List<Relocation> calcRelocations(Stream<? extends Vehicle> rebalancableVehicles, double time);
}
