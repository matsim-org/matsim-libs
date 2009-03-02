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

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

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
		return new EUTScoringFunction(plan, this.params, utilFunc);
	}

}
