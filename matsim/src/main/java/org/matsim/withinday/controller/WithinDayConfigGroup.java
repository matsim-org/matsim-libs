
/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.withinday.controller;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nagel
 *
 */
public final class WithinDayConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "withinDay" ;

	public WithinDayConfigGroup() {
		super(GROUP_NAME);
	}

	// ---
	// ---

	// yyyy I cannot say yet if this should restrict to "TravelTimePrediction".  Probably yes.  Needs to be decided before escalated to xml config.  kai, may'17

	public enum ShortTermPredictionMethod { currentSpeeds }

	ShortTermPredictionMethod predictionMethod = ShortTermPredictionMethod.currentSpeeds ;
	// I think this is what the original code does: use "current" travel times (whatever that means; I still need to find out). kai, may'17

	@SuppressWarnings("unused")
	private static final String SHORT_TERM_PREDICTION_METHOD_CMT="method to predict travel times for the future" ;

	/**
	 * {@value #SHORT_TERM_PREDICTION_METHOD_CMT}
	 *
	 * @return the predictionMethod
	 */
	public ShortTermPredictionMethod getPredictionMethod() {
		return predictionMethod;
	}
	/**
	 * {@value #SHORT_TERM_PREDICTION_METHOD_CMT}
	 *
	 * @param predictionMethod the predictionMethod to set
	 */
	public void setPredictionMethod(ShortTermPredictionMethod predictionMethod) {
		this.predictionMethod = predictionMethod;
	}

	// ---
	// ---

	@Override public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		return comments ;
	}
}
