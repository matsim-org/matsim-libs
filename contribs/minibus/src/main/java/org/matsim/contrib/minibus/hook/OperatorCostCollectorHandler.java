/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.hook;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * 
 * Collects all information needed to calculate the fare.
 * 
 * @author aneumann
 *
 */
final class OperatorCostCollectorHandler implements TransitDriverStartsEventHandler, LinkEnterEventHandler, PersonLeavesVehicleEventHandler, AfterMobsimListener, VehicleAbortsEventHandler, PersonMoneyEventHandler {
	
	private final static Logger log = LogManager.getLogger(OperatorCostCollectorHandler.class);
	
	private Network network;
	private final String pIdentifier;
	private final double costPerVehicleAndDay;
	private final double expensesPerMeter;
	private final double expensesPerSecond;
	
	private final List<OperatorCostContainerHandler> operatorCostContainerHandlerList = new LinkedList<>();
	HashMap<Id<Vehicle>, OperatorCostContainer> vehId2OperatorCostContainer = new HashMap<>();

	OperatorCostCollectorHandler(String pIdentifier, double costPerVehicleAndDay, double expensesPerMeter, double expensesPerSecond){
		this.pIdentifier = pIdentifier;
		this.costPerVehicleAndDay = costPerVehicleAndDay;
		this.expensesPerMeter = expensesPerMeter;
		this.expensesPerSecond = expensesPerSecond;
		log.info("enabled");
	}

	void init(Network network){
		this.network = network;
	}

	void addOperatorCostContainerHandler(OperatorCostContainerHandler operatorCostContainerHandler){
		this.operatorCostContainerHandlerList.add(operatorCostContainerHandler);
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2OperatorCostContainer = new HashMap<>();
		
		for (OperatorCostContainerHandler operatorCostContainerHandler : this.operatorCostContainerHandlerList) {
			operatorCostContainerHandler.reset();
		}
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehId2OperatorCostContainer.put(event.getVehicleId(), new OperatorCostContainer(this.costPerVehicleAndDay, this.expensesPerMeter, this.expensesPerSecond));
		this.vehId2OperatorCostContainer.get(event.getVehicleId()).handleTransitDriverStarts(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.vehId2OperatorCostContainer.containsKey(event.getVehicleId())) {
			// It's a transit driver
			double linkLength = this.network.getLinks().get(event.getLinkId()).getLength();
			this.vehId2OperatorCostContainer.get(event.getVehicleId()).addDistanceTravelled(linkLength);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			// it's a paratransit vehicle
			if(event.getPersonId().toString().contains(this.pIdentifier)){
				// it's the driver
				OperatorCostContainer operatorCostContainer = this.vehId2OperatorCostContainer.remove(event.getVehicleId());
				operatorCostContainer.handleTransitDriverAlights(event);
				
				// call all OperatorCostContainerHandler
				for (OperatorCostContainerHandler operatorCostContainerHandler : this.operatorCostContainerHandlerList) {
					operatorCostContainerHandler.handleOperatorCostContainer(operatorCostContainer);
				}
				
				// Note the operatorCostContainer is dropped at this point.
			}
		}
	}
	
	/**
	 * If the driver agent stucks, no PersonLeavesVehicleEvent will be thrown. Use the 
	 * PersonStuckEvent instead as a replacement to avoid NullPointerExceptions and
	 * calculate an approximate running time cost.
	 */
	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		if (event.getVehicleId().toString().startsWith(this.pIdentifier)) {
			// it's a paratransit vehicle
			log.info("Paratransit vehicle " + event.getVehicleId() + " got stuck at "
					+ Time.writeTime(event.getTime())
					+ ". Operation cost is calculated until the stuck time.");
			OperatorCostContainer operatorCostContainer = this.vehId2OperatorCostContainer.remove(event.getVehicleId());
			operatorCostContainer.handleVehicleAborts(event);

			// call all OperatorCostContainerHandler
			for (OperatorCostContainerHandler operatorCostContainerHandler : this.operatorCostContainerHandlerList) {
				operatorCostContainerHandler.handleOperatorCostContainer(operatorCostContainer);
			}

			// Note the operatorCostContainer is dropped at this point.
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// ok, mobsim is done - finish incomplete entries
		
		// remove non paratransit entries
		List<OperatorCostContainer> entriesToProcess = new LinkedList<>();
		for (OperatorCostContainer operatorCostContainer : this.vehId2OperatorCostContainer.values()) {
			if(operatorCostContainer.getVehicleId().toString().startsWith(this.pIdentifier)){
				// it's a paratransit vehicle
				entriesToProcess.add(operatorCostContainer);
			}
		}
		
		if (entriesToProcess.size() > 0) {
			log.warn("There are " + entriesToProcess.size() + " incomplete entries. Will forward them anyway...");
			for (OperatorCostContainer operatorCostContainer : entriesToProcess) {
				// call all OperatorCostContainerHandler
				for (OperatorCostContainerHandler operatorCostContainerHandler : this.operatorCostContainerHandlerList) {
					operatorCostContainerHandler.handleOperatorCostContainer(operatorCostContainer);
				}
			}
		}
	}

	public Map<Id<Vehicle>, OperatorCostContainer> getVehicleIdToOperatorCostContainerMap(){
		return Collections.unmodifiableMap( this.vehId2OperatorCostContainer );
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		// yyyy what comes below is not so beautiful since it depends on conventions: If some string contains something of some other string,
		// then ... It looks like this was the original design of the minibus contrib, so in principle it should be operated out at some
		// point, but probably not now.  kai, jul'21

		if(event.getPersonId().toString().contains(this.pIdentifier)){
			/* It is a minibus driver. Now find the correct vehicle this person is driving. */
			for(Id<Vehicle> vehicleId : vehId2OperatorCostContainer.keySet()){
				/* The driver Id contains the vehicle Id. For example, if the
				vehicle Id is para_1_2 then you will find that the driver's Id
				is something like pt_para_1_2_para_. I do not know where this
				is set/created, but it may change in the future (JWJ '21) */
				if(vehicleId.toString().contains(this.pIdentifier) &
						event.getPersonId().toString().contains(vehicleId.toString())){
					/* This is the correct vehicle. */

					/* A PersonMoneyEvent assumes a positive value is an income,
					and a negative value is a cost. Keep it consistent here too. */
					vehId2OperatorCostContainer.get(vehicleId).addMoney(event.getAmount());
				}
			}
		}
	}
}
