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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.score;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;

import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingCostCalculatorZH;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

public class ParkingScoreEvaluator {

	private ParkingCostCalculatorZH parkingCostCalculatorZH;
	private ParkingPersonalBetas parkingPersonalBetas;

	public ParkingScoreEvaluator(ParkingCostCalculatorZH parkingCostCalculatorZH, ParkingPersonalBetas parkingPersonalBetas) {
		this.parkingCostCalculatorZH = parkingCostCalculatorZH;
		this.parkingPersonalBetas = parkingPersonalBetas;
	}
	
	public double getParkingScore(ParkingActivityAttributes parkingActAttributes){
		double parkingScore = 0.0;

		// parking cost scoring
		Double parkingCost = getParkingCost(parkingActAttributes.getParkingArrivalTime(), parkingActAttributes.getParkingDuration(), parkingActAttributes.getFacilityId());

		Id personId = parkingActAttributes.getPersonId();
		double costScore = getParkingCostScore(personId, parkingCost);
		parkingScore += costScore;

		// parking walk time
		
		double walkingTimeTotalInMinutes = parkingActAttributes.getTotalWalkDurationInSeconds() / 60.0;
		double activityDuration = parkingActAttributes.getActivityDuration();
		

		double accessAndEgressTimeInMinutes=0;
		if (parkingActAttributes.getFacilityId().toString().contains("gp")){
			accessAndEgressTimeInMinutes = ZHScenarioGlobal.loadDoubleParam("ParkingScoreEvaluator.parkingGarageAccessAndEgressTimeSumInSeconds") / 60.0;
		}
		
		double walkScore = getWalkScore(personId , activityDuration, walkingTimeTotalInMinutes + accessAndEgressTimeInMinutes);
		parkingScore += walkScore;
		
		// parking search time

		double parkingSearchDurationInMinutes = parkingActAttributes.getParkingSearchDurationInSeconds() / 60.0;

		double searchTimeScore = getSearchTimeScore(personId, activityDuration, parkingSearchDurationInMinutes);
		parkingScore += searchTimeScore;
		
		//DebugLib.traceAgent(parkingActAttributes.getDriverId());
		
		if (parkingScore<-700){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		return parkingScore;
	}
	
	private Double getParkingCost(double parkingArrivalTime, double parkingDuration, Id facilityId) {
		return parkingCostCalculatorZH.getParkingCost(facilityId, parkingArrivalTime,
				parkingDuration);
	}
	
	public Double getParkingCost(ParkingActivityAttributes parkingActAttributes) {
		return getParkingCost(parkingActAttributes.getParkingArrivalTime(), parkingActAttributes.getActivityDuration(), parkingActAttributes.getFacilityId());
	}

	private double getParkingCostScore(Id personId, double parkingArrivalTime, double parkingDuration, Id facilityId) {
		Double parkingCost = getParkingCost(parkingArrivalTime, parkingDuration, facilityId);

		// forSettingBreakPoint(facilityId, parkingCost);

		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}

		return getParkingCostScore(personId, parkingCost);
	}

	public double getParkingCostScore(Id personId, Double parkingCost) {
		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}

		return parkingPersonalBetas.getParkingCostBeta(personId) * parkingCost;
	}

	
	private double getWalkScore(Id personId, double activityDuration, double walkingTimeTotalInMinutes) {
		return parkingPersonalBetas.getParkingWalkTimeBeta(personId, activityDuration) * walkingTimeTotalInMinutes;
	}
	
	private double getSearchTimeScore(Id personId, double activityDuration, double parkingSearchTimeInMinutes) {
		return parkingPersonalBetas.getParkingSearchTimeBeta(personId, activityDuration) * parkingSearchTimeInMinutes;
	}

}
