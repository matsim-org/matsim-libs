/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.controller;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParameters;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.analysis.DynamicTravelTimeMatrix;

public class MixedScoringFunctionFactory implements ScoringFunctionFactory {

	private final ScoringParameters params;

	public MixedScoringFunctionFactory(final DynusTConfig dc, final DynamicTravelTimeMatrix ttMatrix, final ActivityToZoneMapping act2zones, final ScoringParameters params) {
		this.params = params;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		return new MixedScoringFunction(this.params);
	}

}
