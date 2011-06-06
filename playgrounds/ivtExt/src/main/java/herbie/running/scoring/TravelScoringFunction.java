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

	public TravelScoringFunction(CharyparNagelScoringParameters params) {
		this.params = params;
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
		
		bikeScore += this.params.constantBike;
		
		bikeScore += travelTime * this.params.marginalUtilityOfTravelingBike_s / 3600d;
		
//		bikeScore += distance * super.params.marginalUtilityOfDistanceBike_m;
		
		return bikeScore;
	}

	protected double getCarScore(double distance, double travelTime) {
		
		double carScore = 0.0;
		
		carScore += this.params.constantCar;
		
		carScore += travelTime * this.params.marginalUtilityOfTraveling_s;
			
//			carScore += this.params.marginalUtilityOfDistanceCar_m * this.params.monetaryDistanceCostRateCar/1000d * dist;
			
		carScore += this.params.marginalUtilityOfDistanceCar_m /1000d * distance;
		
		return carScore;
	}
	
	protected double getWalkScore(double distance, double travelTime) {
		
		double walkScore = 0.0;
		
//		walkScore += super.params.constantWalk;
		
		walkScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s;
		
		walkScore += this.params.marginalUtilityOfDistanceWalk_m * distance;
		
		return walkScore;
	}
	
	protected double getInVehiclePtScore(double distance, double travelTime, double distanceCost) {
		
		double ptScore = 0.0;
		
		ptScore += this.params.constantPt;
		
		double marPt = this.params.marginalUtilityOfTravelingPT_s;
		
		System.out.println();
		
		ptScore += travelTime * this.params.marginalUtilityOfTravelingPT_s;
		
		ptScore += this.params.marginalUtilityOfDistancePt_m * distanceCost / 1000d * distance;
		
		return ptScore;
	}
}
