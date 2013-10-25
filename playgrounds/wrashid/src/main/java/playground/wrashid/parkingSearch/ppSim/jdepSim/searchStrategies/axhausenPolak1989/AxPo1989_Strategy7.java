/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomStreetParkingWithIllegalParkingAndNoLawEnforcement;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

public class AxPo1989_Strategy7 extends RandomStreetParkingWithIllegalParkingAndNoLawEnforcement {

	private double expectedIllegalParkingFeeForWholeDay;

	public AxPo1989_Strategy7(double maxDistance, Network network, double expectedIllegalParkingFeeForWholeDay) {
		super(maxDistance, network);
		this.expectedIllegalParkingFeeForWholeDay = expectedIllegalParkingFeeForWholeDay;
		this.parkingType = "illegalParking";
	}

	@Override
	public String getName() {
		return "AxPo1989_Strategy7";
	}

	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		addIllegalParkingScore(aem);

		super.handleParkingDepartureActivity(aem);
		useSpecifiedParkingType.remove(aem.getPerson().getId());
	}

	private void addIllegalParkingScore(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();

		ParkingActivityAttributes parkingAttributesForScoring = getParkingAttributesForScoring(aem);

		double parkingDuration = GeneralLib.getIntervalDuration(parkingAttributesForScoring.getParkingArrivalTime(),
				aem.getMessageArrivalTime());
		if (parkingAttributesForScoring.getParkingArrivalTime() == aem.getMessageArrivalTime()) {
			parkingDuration = 0;
		}

		double expectedAmountToBePayed = expectedIllegalParkingFeeForWholeDay / (24 * 60 * 60) * parkingDuration;
		scoreInterrupationValue = ZHScenarioGlobal.parkingScoreEvaluator.getParkingCostScore(personId, expectedAmountToBePayed);
	}

	@Override
	public void handleLastParkingScore(AgentWithParking aem) {
		addIllegalParkingScore(aem);

		super.handleLastParkingScore(aem);
	}

}
