/* *********************************************************************** *
 * project: org.matsim.*
 * LegScoringFunction.java
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

package herbie.running.scoring;

import herbie.running.config.HerbieConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


/**
 * 
 * Mai, Juni 2011:
 * 
 * !! Frame of the functions: !!
 * Constant
 * Travel time
 * Travel distance
 *
 * @author bvitins, anhorni
 *
 */
public class TravelScoringFunction {
	
	private CharyparNagelScoringParameters params;
	private HerbieConfigGroup herbieConfigGroup;

	public TravelScoringFunction(CharyparNagelScoringParameters params, HerbieConfigGroup ktiConfigGroup) {
		this.params = params;
		this.herbieConfigGroup = ktiConfigGroup;
	}
	
	/**
	 * Uses the same values like car mode
	 * @param leg
	 * @param travelTime
	 * @return
	 */
	public double getAlternativeModeScore(double distance, double travelTime) {
		
		return this.getCarScore(distance, travelTime);
	}

	public double getBikeScore(double distance, double travelTime) {
		
		double bikeScore = 0.0;
		
		double timeThreashold = 20.0 * 60.0; // sec
		
		bikeScore += this.params.modeParams.get(TransportMode.bike).constant;
		
		if(travelTime <= timeThreashold) {
			bikeScore += travelTime * this.params.modeParams.get(TransportMode.bike).marginalUtilityOfTraveling_s;
		}
		else{
			bikeScore += travelTime * this.params.modeParams.get(TransportMode.bike).marginalUtilityOfTraveling_s + 
				3 * (travelTime - timeThreashold) * this.params.modeParams.get(TransportMode.bike).marginalUtilityOfTraveling_s;
		}
		
		bikeScore += distance * this.herbieConfigGroup.getMarginalDistanceCostRateBike() * this.params.marginalUtilityOfMoney;
		
		return bikeScore;
	}

	public double getCarScore(double distance, double travelTime) {
		
		double carScore = 0.0;
		
		carScore += this.params.modeParams.get(TransportMode.car).constant;
		
		carScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s;
			
//			carScore += this.params.marginalUtilityOfDistanceCar_m * this.params.monetaryDistanceCostRateCar/1000d * dist;
			
		carScore += this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * distance;
		
		return carScore;
	}
	
	public double getWalkScore(double distance, double travelTime) {
		
		double walkScore = 0.0;
		
//		walkScore += super.params.constantWalk;
		
		double timeThreshold1 = 20.0 * 60.0; // sec
		double timeThreshold2 = 60.0 * 60.0; // sec
		
//		if(travelTime <= timeThreashold1) {
//			walkScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s;
//		}
//		else{
//			walkScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s+ 
//				6 * (travelTime - timeThreashold1) * this.params.marginalUtilityOfTravelingWalk_s;
//		}
		
		
		walkScore += travelTime * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s;
		
		if(travelTime > timeThreshold1) 
		{
			walkScore += 40d * (travelTime - timeThreshold1) * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s;
		}
		if (travelTime > timeThreshold2) 
		{
			walkScore += 0.3 * (travelTime - timeThreshold2) * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s;
		}
		
//		walkScore += Math.pow(travelTime, 2) * this.params.marginalUtilityOfTravelingWalk_s;
		
		walkScore += this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m * distance;
		
		return walkScore;
	}
	/**
	 * 
	 * @param distance
	 * @param travelTime
	 * @param distanceCost is a factor for transit travel cards
	 * @return
	 */
	public double getInVehiclePtScore(double distance, double travelTime, double distanceCost) {
		
		double ptScore = 0.0;
		
		ptScore += this.params.modeParams.get(TransportMode.pt).constant;
		
		double marPt = this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s;
		
		ptScore += travelTime * this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s;
		
		ptScore += this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * distanceCost * distance;
		
		return ptScore;
	}
}
