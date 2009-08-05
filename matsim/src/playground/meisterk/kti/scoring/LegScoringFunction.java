/* *********************************************************************** *
 * project: org.matsim.*
 * LegScoringFunction.java
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

package playground.meisterk.kti.scoring;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.meisterk.org.matsim.config.groups.KtiConfigGroup;


/**
 * This class contains modifications of the standard leg scoring function for the KTI project.
 * 
 * @author meisterk
 *
 */
public class LegScoringFunction extends
		org.matsim.core.scoring.charyparNagel.LegScoringFunction {

	private final KtiConfigGroup ktiConfigGroup;

	public LegScoringFunction(PlanImpl plan,
			CharyparNagelScoringParameters params,
			KtiConfigGroup ktiConfigGroup) {
		super(plan, params);
		this.ktiConfigGroup = ktiConfigGroup;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			LegImpl leg) {

		double legScore = super.calcLegScore(departureTime, arrivalTime, leg);
		
		if (leg.getMode().equals(TransportMode.bike)) {
			legScore += this.ktiConfigGroup.getConstBike();
		}
		
		return legScore;
	}
	
	
}
