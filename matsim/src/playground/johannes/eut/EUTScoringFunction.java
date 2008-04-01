/* *********************************************************************** *
 * project: org.matsim.*
 * EUTScoringFunction.java
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

import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.scoring.CharyparNagelScoringFunction;

/**
 * @author illenberger
 *
 */
public class EUTScoringFunction extends CharyparNagelScoringFunction {

	private ArrowPrattRiskAversionI utilFunc;
	
	public EUTScoringFunction(Plan plan, ArrowPrattRiskAversionI utilFunc) {
		super(plan);
		this.utilFunc = utilFunc;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			Leg leg) {
		return marginalUtilityOfTraveling * utilFunc.evaluate(arrivalTime - departureTime);
	}

}
