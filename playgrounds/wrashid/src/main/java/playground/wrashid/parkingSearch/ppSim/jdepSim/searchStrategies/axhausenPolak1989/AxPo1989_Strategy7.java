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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomStreetParkingWithIllegalParkingAndNoLawEnforcement;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

public class AxPo1989_Strategy7 extends RandomStreetParkingWithIllegalParkingAndNoLawEnforcement {

	public AxPo1989_Strategy7(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
		this.parkingType = "illegalParking";
	}

	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		ParkingActivityAttributes parkingAttributesForScoring = getParkingAttributesForScoring(aem);

		addIllegalParkingScore(aem, parkingAttributesForScoring.getParkingArrivalTime(), aem.getMessageArrivalTime());

		super.handleParkingDepartureActivity(aem);
		useSpecifiedParkingType.remove(aem.getPerson().getId());
	}

	private void addIllegalParkingScore(AgentWithParking aem, double parkingArrivalTime, double parkingDepartureTime) {
		double expectedIllegalParkingFeeForWholeDay;
		Id personId = aem.getPerson().getId();

		Id currentParkingId = AgentWithParking.parkingManager.getCurrentParkingId(personId);
		if (currentParkingId.toString().contains("illegal")) {

			
			double parkingDuration = GeneralLib.getIntervalDuration(parkingArrivalTime, parkingDepartureTime);
			if (parkingArrivalTime == parkingDepartureTime) {
				parkingDuration = 0;
			}

			Coord coordinatesLindenhofZH = ParkingHerbieControler.getCoordinatesLindenhofZH();
			
			//Link parkingLink = ZHScenarioGlobal.scenario.getNetwork().getLinks().get(AgentWithParking.parkingManager.getLinkOfParking(currentParkingId));
			
			if (GeneralLib.getDistance(AgentWithParking.parkingManager.getParkingsHashMap().get(currentParkingId).getCoord(), coordinatesLindenhofZH)<ZHScenarioGlobal.loadDoubleParam("AxPo1989_Strategy7.radius")){
				expectedIllegalParkingFeeForWholeDay = ZHScenarioGlobal.loadDoubleParam("AxPo1989_Strategy7.expectedIllegalParkingFeeForWholeDayInsideCircle");
			} else {
				expectedIllegalParkingFeeForWholeDay = ZHScenarioGlobal.loadDoubleParam("AxPo1989_Strategy7.expectedIllegalParkingFeeForWholeDayOutsideCircle");
			}

			double expectedAmountToBePayed = expectedIllegalParkingFeeForWholeDay / (24 * 60 * 60) * parkingDuration;
			scoreInterrupationValue += ZHScenarioGlobal.parkingScoreEvaluator
					.getParkingCostScore(personId, expectedAmountToBePayed);

		}

		if (scoreInterrupationValue == 0) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
	}

	@Override
	public void handleLastParkingScore(AgentWithParking aem) {
		Activity firstAct = (Activity) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getIndexOfFirstCarLegOfDay() - 3);

		addIllegalParkingScore(aem, aem.getMessageArrivalTime(), firstAct.getEndTime());

		super.handleLastParkingScore(aem);
	}

}
