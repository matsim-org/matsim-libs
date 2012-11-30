/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAndMoneyDependentScoringFunction.java
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

package playground.christoph.socialcosts;

import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunction;

/**
 * Also the scoring function has to take social costs into account. Otherwise
 * scoring and routing would not be consistent.
 * Since social costs are (so far) measured in time, also added money should be
 * measured in seconds.
 * 
 * @author cdobler
 */
public class TimeAndMoneyDependentScoringFunction extends OnlyTravelTimeDependentScoringFunction {

	@Override
	public void addMoney(final double amount) {
		score += amount;
	}
}
