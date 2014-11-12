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

package playground.ikaddoura.noise2;

/**
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseConfigParameters {
	
	public static double getAnnualCostRate(){
		final double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));
		return annualCostRate;
	}
	
	public static double getTimeBinSizeNoiseComputation(){
		final double timeBinSize = 3600.0;
		return timeBinSize;
	}
	
	public static double getTimeBinSizeRouter(){
		final double timeBinSize = 3600.0;
		return timeBinSize;
	}
	
	public static double getScaleFactor(){
		final double scaleFactor = 10.;
		return scaleFactor;
	}
	
	// distance between two receiver points along x- and y-axes
	public static double getReceiverPointGap(){
		final double receiverPointGap = 250.;
		return receiverPointGap;
	}
	
	// radius around a receiver point in which all links are considered as relevant
	public static double getRelevantRadius(){
		final double relevantRadius = 500.;
		return relevantRadius;
	}
	
}
