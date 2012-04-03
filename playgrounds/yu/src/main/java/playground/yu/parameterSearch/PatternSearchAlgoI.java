/* *********************************************************************** *
 * project: org.matsim.*
 * PatternSearchAlgoI.java
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

public interface PatternSearchAlgoI {
	/** @return the new suggestion of parameter set */
	double[] getTrial();

	/**
	 * should be called by or "after" {@code AfterMobsimListener}
	 * 
	 * @param objective
	 *            the value of objective function by the newest suggestion of
	 *            parameter set
	 */
	void setObjective(double objective);

	/** should be called inside setObjective(double) */
	void createTrial();
}
