/* *********************************************************************** *
 * project: org.matsim.*
 * PatternSearchListener.java
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

package playground.yu.parameterSearch;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.controler.listener.ControlerListener;

public interface PatternSearchListenerI extends ControlerListener {
	static final String LOWER_BOUNDARY_OF_PARAMETER_ = "lowerBoundaryOfParameter_",
			UPPER_BOUNDARY_OF_PARAMETER_ = "upperBoundaryOfParameter_",
			CRITERION = "criterion", MAX_ITER = "maxIter";

	static final Map<String, Double> nameParametersMap = new TreeMap<String, Double>();

	static final String PERFORMING = "performing", TRAVELING = "traveling",
			TRAVELING_PT = "travelingPt", TRAVELING_WALK = "travelingWalk";

	static final String MARGINAL_UTL_OF_DISTANCE_WALK = "marginalUtlOfDistanceWalk",
			MONETARY_DISTANCE_COST_RATE_CAR = "monetaryDistanceCostRateCar",
			MONETARY_DISTANCE_COST_RATE_PT = "monetaryDistanceCostRatePt";

	static final String CONSTANT_CAR = "constantCar",
			CONSTANT_WALK = "constantWalk", CONSTANT_PT = "constantPt";

	static final String STUCK = "stuck",
			CONSTANT_LEFT_TURN = "constantLeftTurn";
}
