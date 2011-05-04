/* *********************************************************************** *
 * project: org.matsim.*
 * BseStrategyManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelTime;

import cadyts.calibrators.Calibrator;

/**
 * @author yu
 *
 */
public interface BseStrategyManager {
	public static String UTILITY_CORRECTION = "utilityCorrection";

	/*
	 * corrects the scores of all the plans except the one selected in the
	 * choice set of a Person
	 *
	 * @param person
	 *
	 * public void correctPlansUtilities(final PersonImpl person);
	 */

	/**
	 * initialization of BseStrategyManager
	 *
	 * @param calibrator
	 * @param travelTimes
	 */
	public void init(final Calibrator<Link> calibrator,
			final TravelTime travelTimes);

}
