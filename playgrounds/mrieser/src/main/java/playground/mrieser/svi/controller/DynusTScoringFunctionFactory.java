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
import org.matsim.deprecated.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.deprecated.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.analysis.DynamicTravelTimeMatrix;

/**
 * @author mrieser
 */
public class DynusTScoringFunctionFactory implements ScoringFunctionFactory {

	private final DynusTConfig dc;
	private final DynamicTravelTimeMatrix ttMatrix;
	private final ActivityToZoneMapping act2zones;
	private final ScoringParameters params;
	
	public DynusTScoringFunctionFactory(final DynusTConfig dc, final DynamicTravelTimeMatrix ttMatrix, final ActivityToZoneMapping act2zones, final ScoringParameters params) {
		this.dc = dc;
		this.ttMatrix = ttMatrix;
		this.act2zones = act2zones;
		this.params = params;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		return new DynusTScoringFunction(person.getSelectedPlan(), this.ttMatrix, this.act2zones,
				new CharyparNagelLegScoring(this.params, null),
				new CharyparNagelActivityScoring(this.params));
	}

}
