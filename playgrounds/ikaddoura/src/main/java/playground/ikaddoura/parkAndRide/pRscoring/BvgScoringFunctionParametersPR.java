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

package playground.ikaddoura.parkAndRide.pRscoring;

import org.apache.log4j.Logger;

/**
 * Scoring function parameters of {@link BvgLegScoringFunctionPR}
 * 
 * @author aneumann, adjusted for P+R by ikaddoura
 *
 */
public class BvgScoringFunctionParametersPR {
	
	private static final Logger log = Logger.getLogger(BvgScoringFunctionParametersPR.class);
	
	public double offsetCar;
	public double offsetPt;
	public double offsetRide;
	public double offsetBike;
	public double offsetWalk;
	
	public double betaOffsetCar;
	public double betaOffsetPt;
	public double betaOffsetRide;
	public double betaOffsetBike;
	public double betaOffsetWalk;
	
	public double monetaryDistanceCostRateRide;
	public double monetaryDistanceCostRateBike;
	public double monetaryDistanceCostRateWalk;
	
	public double intermodalTransferPenalty;

	public BvgScoringFunctionParametersPR(BvgScoringFunctionConfigGroupPR bvgConfig) {
		
		log.info("Started...");
		log.info("Don't know the reason for this class to exist. Needs to be checked.");
		
		this.offsetCar = bvgConfig.getOffsetCar();
		this.offsetPt = bvgConfig.getOffsetPt();
		this.offsetRide = bvgConfig.getOffsetRide();
		this.offsetBike = bvgConfig.getOffsetBike();
		this.offsetWalk = bvgConfig.getOffsetWalk();
		
		this.betaOffsetCar = bvgConfig.getBetaOffsetCar();
		this.betaOffsetPt = bvgConfig.getBetaOffsetPt();
		this.betaOffsetRide = bvgConfig.getBetaOffsetRide();
		this.betaOffsetBike = bvgConfig.getBetaOffsetBike();
		this.betaOffsetWalk = bvgConfig.getBetaOffsetWalk();
		
		this.monetaryDistanceCostRateRide = bvgConfig.getMonetaryDistanceCostRateRide();
		this.monetaryDistanceCostRateBike = bvgConfig.getMonetaryDistanceCostRateBike();
		this.monetaryDistanceCostRateWalk = bvgConfig.getMonetaryDistanceCostRateWalk();
		
		this.intermodalTransferPenalty = bvgConfig.getInterModalTransferPenalty();
		
		// do additional calculations here	
		
	}


}
