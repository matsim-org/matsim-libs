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

import org.matsim.core.scoring.CharyparNagelScoringParameters;


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
	protected double getAlternativeModeScore(double distance, double travelTime) {
		
		return this.getCarScore(distance, travelTime);
	}

	protected double getBikeScore(double distance, double travelTime) {
		
		double bikeScore = 0.0;
		
		double timeThreashold = 20.0 * 60.0; // sec
		
		bikeScore += this.params.constantBike;
		
		if(travelTime <= timeThreashold) {
			bikeScore += travelTime * this.params.marginalUtilityOfTravelingBike_s;
		}
		else{
			bikeScore += travelTime * this.params.marginalUtilityOfTravelingBike_s + 
				3 * (travelTime - timeThreashold) * this.params.marginalUtilityOfTravelingBike_s;
		}
		
		bikeScore += distance * this.herbieConfigGroup.getMarginalDistanceCostRateBike() * this.params.marginalUtilityOfMoney;
		
		return bikeScore;
	}

	protected double getCarScore(double distance, double travelTime) {
		
		double carScore = 0.0;
		
		carScore += this.params.constantCar;
		
		carScore += travelTime * this.params.marginalUtilityOfTraveling_s;
			
//			carScore += this.params.marginalUtilityOfDistanceCar_m * this.params.monetaryDistanceCostRateCar/1000d * dist;
			
		carScore += this.params.marginalUtilityOfDistanceCar_m * distance;
		
		return carScore;
	}
	
	protected double getWalkScore(double distance, double travelTime) {
		
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
		
		
		walkScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s;
		
		if(travelTime > timeThreshold1) 
		{
			walkScore += 40d * (travelTime - timeThreshold1) * this.params.marginalUtilityOfTravelingWalk_s;
		}
		if (travelTime > timeThreshold2) 
		{
			walkScore += 0.3 * (travelTime - timeThreshold2) * this.params.marginalUtilityOfTravelingWalk_s;
		}
		
//		walkScore += Math.pow(travelTime, 2) * this.params.marginalUtilityOfTravelingWalk_s;
		
		walkScore += this.params.marginalUtilityOfDistanceWalk_m * distance;
		
		return walkScore;
	}
	/**
	 * 
	 * @param distance
	 * @param travelTime
	 * @param distanceCost is a factor for transit travel cards
	 * @return
	 */
	protected double getInVehiclePtScore(double distance, double travelTime, double distanceCost) {
		
		double ptScore = 0.0;
		
		ptScore += this.params.constantPt;
		
		double marPt = this.params.marginalUtilityOfTravelingPT_s;
		
		ptScore += travelTime * this.params.marginalUtilityOfTravelingPT_s;
		
		ptScore += this.params.marginalUtilityOfDistancePt_m * distanceCost * distance;
		
		return ptScore;
	}
}
