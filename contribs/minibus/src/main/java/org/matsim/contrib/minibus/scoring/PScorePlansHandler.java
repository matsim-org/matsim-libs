/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.scoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.fare.StageContainer;
import org.matsim.contrib.minibus.fare.StageContainerHandler;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.vehicles.Vehicle;

/**
 * Scores paratransit vehicles
 * 
 * @author aneumann
 *
 */
public final class PScorePlansHandler implements StageContainerHandler, OperatorCostContainerHandler{
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(PScorePlansHandler.class);
	
	private final TicketMachineI ticketMachine;
	private Map<Id<Vehicle>, PScoreContainer> vehicleId2ScoreMap = new ConcurrentHashMap<>();

	public PScorePlansHandler(TicketMachineI ticketMachine){
		this.ticketMachine = ticketMachine;
	}
	
	public Map<Id<Vehicle>, PScoreContainer> getDriverId2ScoreMap() {
		return this.vehicleId2ScoreMap;
	}

	@Override
	public void handleFareContainer(StageContainer fareContainer) {
		if (this.vehicleId2ScoreMap.get(fareContainer.getVehicleId()) == null) {
			this.vehicleId2ScoreMap.put(fareContainer.getVehicleId(), new PScoreContainer(fareContainer.getVehicleId(), this.ticketMachine));
		}

		// TODO debugging, got a single null pointer here?! 
		try {
			this.vehicleId2ScoreMap.get(fareContainer.getVehicleId()).handleStageContainer(fareContainer);
		} catch (Exception e) {
			log.warn("Got a null pointer here...");
			log.warn(e.getMessage());
			log.warn("FareContainer " + fareContainer);
			log.warn("vehicleMap " + this.vehicleId2ScoreMap);
			e.printStackTrace();
		}
	}

    @Override
	public void handleOperatorCostContainer(OperatorCostContainer operatorCostContainer) {
		if (this.vehicleId2ScoreMap.get(operatorCostContainer.getVehicleId()) == null) {
			this.vehicleId2ScoreMap.put(operatorCostContainer.getVehicleId(), new PScoreContainer(operatorCostContainer.getVehicleId(), this.ticketMachine));
		}
		
		// TODO debugging, got a single null pointer here?! 
		try {
			this.vehicleId2ScoreMap.get(operatorCostContainer.getVehicleId()).handleOperatorCostContainer(operatorCostContainer);
		} catch (Exception e) {
			log.warn("Got a null pointer here...");
			log.warn(e.getMessage());
			log.warn("CostContainer " + operatorCostContainer);
			log.warn("vehicleMap " + this.vehicleId2ScoreMap);
			e.printStackTrace();
		}
			
	}

	@Override
	public void reset() {
		this.vehicleId2ScoreMap = new ConcurrentHashMap<>();
	}
}