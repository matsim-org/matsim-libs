/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgScoringFunction;

import org.apache.log4j.Logger;

/**
 * Scoring function parameters of {@link BvgLegScoringFunction}
 * 
 * @author aneumann
 *
 */
public class BvgScoringFunctionParameters {
	
	private static final Logger log = Logger.getLogger(BvgScoringFunctionParameters.class);
	
	public double offsetCar;
	public double offsetPt;
	public double offsetRide;
	public double offsetBike;
	public double offsetWalk;
	
	public double monetaryDistanceCostRateRide;
	public double monetaryDistanceCostRateBike;
	public double monetaryDistanceCostRateWalk;

	public BvgScoringFunctionParameters(BvgScoringFunctionConfigGroup bvgConfig) {
		
		log.info("Started...");
		log.info("Don't know the reason for this class to exist. Needs to be checked.");
		
		this.offsetCar = bvgConfig.getOffsetCar();
		this.offsetPt = bvgConfig.getOffsetPt();
		this.offsetRide = bvgConfig.getOffsetRide();
		this.offsetBike = bvgConfig.getOffsetBike();
		this.offsetWalk = bvgConfig.getOffsetWalk();
		
		this.monetaryDistanceCostRateRide = bvgConfig.getMonetaryDistanceCostRateRide();
		this.monetaryDistanceCostRateBike = bvgConfig.getMonetaryDistanceCostRateBike();
		this.monetaryDistanceCostRateWalk = bvgConfig.getMonetaryDistanceCostRateWalk();
		
		// do additional calculations here	
		
	}


}
