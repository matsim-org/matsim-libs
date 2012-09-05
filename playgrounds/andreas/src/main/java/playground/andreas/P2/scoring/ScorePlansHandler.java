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

package playground.andreas.P2.scoring;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.andreas.P2.scoring.fare.FareContainer;
import playground.andreas.P2.scoring.fare.FareContainerHandler;
import playground.andreas.P2.scoring.operator.OperatorCostContainer;
import playground.andreas.P2.scoring.operator.OperatorCostContainerHandler;

/**
 * Scores paratransit vehicles
 * 
 * @author aneumann
 *
 */
public class ScorePlansHandler implements FareContainerHandler, OperatorCostContainerHandler{
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(ScorePlansHandler.class);
	
	TreeMap<Id, ScoreContainer> vehicleId2ScoreMap = new TreeMap<Id, ScoreContainer>();

	public TreeMap<Id, ScoreContainer> getDriverId2ScoreMap() {
		return this.vehicleId2ScoreMap;
	}

	@Override
	public void handleFareContainer(FareContainer fareContainer) {
		if (this.vehicleId2ScoreMap.get(fareContainer.getVehicleId()) == null) {
			this.vehicleId2ScoreMap.put(fareContainer.getVehicleId(), new ScoreContainer(fareContainer.getVehicleId()));
		}
		
		this.vehicleId2ScoreMap.get(fareContainer.getVehicleId()).handleFareContainer(fareContainer);
	}

	@Override
	public void handleOperatorCostContainer(OperatorCostContainer operatorCostContainer) {
		if (this.vehicleId2ScoreMap.get(operatorCostContainer.getVehicleId()) == null) {
			this.vehicleId2ScoreMap.put(operatorCostContainer.getVehicleId(), new ScoreContainer(operatorCostContainer.getVehicleId()));
		}
		
		this.vehicleId2ScoreMap.get(operatorCostContainer.getVehicleId()).handleOperatorCostContainer(operatorCostContainer);
	}

	@Override
	public void reset(int iteration) {
		this.vehicleId2ScoreMap = new TreeMap<Id, ScoreContainer>();
	}
}