/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingchoice.PC2.scoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

/**
 * The parking scoring function adds a term to the original scoring function
 * (creates wrapper around original scoring function factory).
 * 
 * rashid_waraich
 * 
 */

public class ParkingScoringFunctionFactory implements ScoringFunctionFactory {

	private ScoringFunctionFactory orginalScoringFunctionFactory;
	private ParkingScore parkingScoreManager;

	public ParkingScoringFunctionFactory(
			ScoringFunctionFactory orginalScoringFunction, ParkingScore parkingScoreManager) {
		this.orginalScoringFunctionFactory = orginalScoringFunction;
		this.parkingScoreManager = parkingScoreManager;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = (SumScoringFunction) orginalScoringFunctionFactory
				.createNewScoringFunction(person);
		scoringFunctionSum.addScoringFunction(new ParkingScoringFunction(person
				.getSelectedPlan(),parkingScoreManager));
		return scoringFunctionSum;
	}

}
