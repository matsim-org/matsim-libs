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

package playground.wrashid.parkingSearch.withindayFW.util;

import org.matsim.contrib.parking.lib.DebugLib;

public class GlobalParkingSearchParams {

	private static int scenarioId=-1;
	private static double populationPercentage;
	private static double parkingScoreWeight;
	private static int detailedOutputAfterIteration;
	
	public static int getScenarioId() {
		return scenarioId;
	}
	
	public static double getPopulationPercentage() {
		return populationPercentage;
	}
	
	public static double getParkingScoreWeight() {
		return parkingScoreWeight;
	}

	public static void setScenarioId(int scenarioId) {
		if (GlobalParkingSearchParams.scenarioId!=-1){
			DebugLib.stopSystemAndReportInconsistency("scenarioId already set!");
		}
		
		GlobalParkingSearchParams.scenarioId = scenarioId;
	}

	public static void setPopulationPercentage(double populationPercentage) {
		GlobalParkingSearchParams.populationPercentage = populationPercentage;
	}

	public static void setParkingScoreWeight(double parkingScoreWeight) {
		GlobalParkingSearchParams.parkingScoreWeight = parkingScoreWeight;
	}

	public static boolean writeDetailedOutput(int iterationNumber) {
		if (detailedOutputAfterIteration<=0){
			return false;
		}
		
		return iterationNumber%detailedOutputAfterIteration==0;
	}

	public static void setDetailedOutputAfterIteration(int detailedOutputAfterIteration) {
		GlobalParkingSearchParams.detailedOutputAfterIteration = detailedOutputAfterIteration;
	}
	
}
