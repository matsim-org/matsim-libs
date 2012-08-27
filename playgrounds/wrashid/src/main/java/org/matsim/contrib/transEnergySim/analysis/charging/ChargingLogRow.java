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

package org.matsim.contrib.transEnergySim.analysis.charging;

import org.matsim.api.core.v01.Id;

public class ChargingLogRow {
	Id agentId;
	Id linkIdOrFacilityId;
	double startChargingTime;
	double endChargingTime;
	double energyChargedInJoule;

	public ChargingLogRow(Id agentId, Id linkIdOrFacilityId, double startChargingTime, double endChargingTime,
			double energyChargedInJoule) {
		super();
		this.agentId = agentId;
		this.linkIdOrFacilityId = linkIdOrFacilityId;
		this.startChargingTime = startChargingTime;
		this.endChargingTime = endChargingTime;
		this.energyChargedInJoule = energyChargedInJoule;
	}

	public Id getAgentId() {
		return agentId;
	}

	public Id getLinkIdOrFacilityId() {
		return linkIdOrFacilityId;
	}

	public double getStartChargingTime() {
		return startChargingTime;
	}

	public double getEndChargingTime() {
		return endChargingTime;
	}

	public double getEnergyChargedInJoule() {
		return energyChargedInJoule;
	}

}
