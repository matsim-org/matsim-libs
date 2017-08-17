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

import java.util.*;

import org.matsim.contrib.dvrp.data.Vehicle;

/**
 * This is just an example how to implement RebalancingStrategy, not a real rebalancing strategy.
 * 
 * @author michalm
 */
public class SendToStartLinkStrategy implements RebalancingStrategy {
	@Override
	public List<Relocation> calcRelocations(Iterable<? extends Vehicle> rebalancableVehicles, double time) {
		List<Relocation> relocations = new ArrayList<>();
		for (Vehicle v : rebalancableVehicles) {
			relocations.add(new Relocation(v, v.getStartLink()));
		}
		return relocations;
	}
}
