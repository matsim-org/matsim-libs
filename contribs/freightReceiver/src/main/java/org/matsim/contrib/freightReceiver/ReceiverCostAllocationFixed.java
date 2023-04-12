/**
 * ********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 * *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 * LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 * *
 * *********************************************************************** *
 * *
 * This program is free software; you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation; either version 2 of the License, or     *
 * (at your option) any later version.                                   *
 * See also COPYING, LICENSE and WARRANTY file                           *
 * *
 * ***********************************************************************
 */
package org.matsim.contrib.freightReceiver;

import org.matsim.contrib.freight.carrier.Carrier;

/**
 * A simple implementation where the cost allocated by the {@link Carrier} to
 * the {@link Receiver} is a fixed value.
 */
class ReceiverCostAllocationFixed implements ReceiverCostAllocation{

	private final double cost;

	ReceiverCostAllocationFixed(double cost){
		this.cost = cost;
	}
	@Override
	public double getCost(Carrier carrier, Receiver receiver) {
		return this.cost;
	}
}
