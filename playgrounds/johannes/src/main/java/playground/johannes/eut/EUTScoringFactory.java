/* *********************************************************************** *
 * project: org.matsim.*
 * EUTScoringFactory.java
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

/**
 * 
 */
package playground.johannes.eut;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

/**
 * @author illenberger
 *
 */
public class EUTScoringFactory implements ScoringFunctionFactory {

	private ArrowPrattRiskAversionI utilFunc;
	private final CharyparNagelScoringParameters params;

	public EUTScoringFactory(ArrowPrattRiskAversionI utilFunc, final CharyparNagelScoringConfigGroup config) {
		this.utilFunc = utilFunc;
		this.params = new CharyparNagelScoringParameters(config);
	}
	
	public ScoringFunction getNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, params));
		scoringFunctionAccumulator.addScoringFunction(new EUTScoringFunction(plan, params, utilFunc));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(params));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}

}
