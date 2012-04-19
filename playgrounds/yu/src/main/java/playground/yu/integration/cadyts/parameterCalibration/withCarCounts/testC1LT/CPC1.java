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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;

import cadyts.calibrators.analytical.ChoiceParameterCalibrator;
import cadyts.demand.Plan;

public class CPC1<L> extends ChoiceParameterCalibrator<L> {

	private static final long serialVersionUID = 1L;

	public CPC1(String logFile, Long randomSeed, int timeBinSize_s,
			int parameterDimension) {
		super(logFile, randomSeed, timeBinSize_s, parameterDimension);
	}

	public double getUtilityCorrection(final Plan<L> plan) {
		return super.calcLinearPlanEffect(plan);
	}
}
