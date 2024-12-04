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

package org.matsim.freight.receiver;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.Carrier;

import java.util.*;

/**
 * A cost allocation model where the {@link Carrier} distributes its cost
 * equally among the {@link Receiver}s it services.
 */
class ReceiverCostAllocationEqualProportion implements ReceiverCostAllocation{
	final Map<Id<Carrier>, Map<Id<Receiver>, Double>> costMap;

	@Inject
	Scenario scenario;

	ReceiverCostAllocationEqualProportion(){
		this.costMap = new TreeMap<>();
	}

	@Override
	public double getScore(Carrier carrier, Receiver receiver) {
		if(!costMap.containsKey(carrier.getId())){
			/* This carrier has not been processed before. */
			Map<Id<Receiver>, Double> carrierMap = new TreeMap<>();
			double carrierScore = carrier.getSelectedPlan().getScore();

			/* Find all the receivers. */
			List<Id<Receiver>> receiversServiced = findAllReceiversServicedByThisCarrier(carrier.getId());
			double scorePerReceiver = carrierScore / ((double)receiversServiced.size());
			for(Id<Receiver> receiverId : receiversServiced){
				carrierMap.put(receiverId, scorePerReceiver);
			}
			costMap.put(carrier.getId(), carrierMap);
		}

		if(!costMap.get(carrier.getId()).containsKey(receiver.getId())){
			throw new IllegalStateException("The cost allocation map is not consistent.");
		}

		return costMap.get(carrier.getId()).get(receiver.getId());
	}

	@Override
	public void reset() {
		this.costMap.clear();
	}

	private List<Id<Receiver>> findAllReceiversServicedByThisCarrier(Id<Carrier> carrierId){
		List<Id<Receiver>> receiversServiced = new ArrayList<>();
		Receivers receivers = ReceiverUtils.getReceivers(scenario);
		for(Receiver receiver : receivers.getReceivers().values()){
			Iterator<ReceiverOrder> orderIterator = receiver.getSelectedPlan().getReceiverOrders().iterator();
			boolean found = false;
			while(!found && orderIterator.hasNext()){
				if(orderIterator.next().getCarrierId().equals(carrierId)){
					found = true;
					receiversServiced.add(receiver.getId());
				}
			}
		}
		return receiversServiced;
	}


}
