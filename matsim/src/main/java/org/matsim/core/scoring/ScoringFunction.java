/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring;

/**
 * A scoring function calculates the score for one plan of an agent.  The score
 * usually depends on how much time an agent is traveling and how much time an
 * agent spends at an activity.  Thus the scoring function gets informed when
 * activities start and end as well as when legs start and end.<br>
 * Note that one ScoringFunction calculates the score for exactly one agent.
 * Thus every agents must have its own instance of a scoring function!
 *
 * @author mrieser
 */
public interface ScoringFunction extends PersonExperienceListener {


	/**
	 * Returns the score for this plan.

	 * @return the score
	 */
	public double getScore();


}
