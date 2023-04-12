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
 * A cost allocation model where the {@link Carrier} distributes its cost
 * equally among the {@link Receiver}s it services.
 */
class ReceiverCostAllocationEqualProportion implements ReceiverCostAllocation{

	private boolean built = false;
	@Override
	public double getCost(Carrier carrier, Receiver receiver) {
		if(!built){
			setup(carrier, receiver);
		}
		return 0;
	}

	private void setup(Carrier carrier, Receiver receiver){

	}
}
