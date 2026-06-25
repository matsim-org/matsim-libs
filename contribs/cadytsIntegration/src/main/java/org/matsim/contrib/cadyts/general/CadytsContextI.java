/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsContextI.java
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
package org.matsim.contrib.cadyts.general;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import org.matsim.api.core.v01.population.Person;

/**
 * @author nagel
 *
 */
public interface CadytsContextI<T> {

	AnalyticalCalibrator<T> getCalibrator() ;

	PlansTranslator<T> getPlansTranslator();

	/**
	 * Returns the weight (e.g. PCU) of the agent for calibration purposes.
	 * Default is 1.0 (standard vehicle or PT passenger).
	 */
	default double getAgentWeight(Person person) {
		return 1.0;
	}
}
