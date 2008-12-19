/* *********************************************************************** *
 * project: org.matsim.*																															*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.benjamin;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;


/**
 * @author dgrether
 *
 */
public class BKickScoringFunctionFactory implements ScoringFunctionFactory {

	private CharyparNagelScoringConfigGroup configGroup;

	public BKickScoringFunctionFactory(
			CharyparNagelScoringConfigGroup charyparNagelScoring) {
		this.configGroup = charyparNagelScoring;
	}

	/**
	 * @see org.matsim.scoring.ScoringFunctionFactory#getNewScoringFunction(org.matsim.population.Plan)
	 */
	public ScoringFunction getNewScoringFunction(Plan plan) {
		return new BKickScoringFunction(plan, this.configGroup);
	}

}
