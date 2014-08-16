/* *********************************************************************** *
 * project: org.matsim.*
 * SnapshotWriterRegistrar
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.controler;

import org.matsim.core.replanning.modules.*;


public class PlanStrategyRegistrar {
	public static enum Selector { KeepLastSelected, BestScore, ChangeExpBeta, SelectExpBeta, SelectRandom, SelectPathSizeLogit } 
	public static enum Names { ReRoute, TimeAllocationMutator, ChangeLegMode } 
	// yy Using enums here is against the design: This is default input to a registry, which is by default extensible, and enums
	// are not extensible.  Using enums was triggered since I (kn) needed to have an exhaustive list of non-innovative modules
	// (the Names).  A much better
	// solution would be to take this from the PlanStrategy directly, by checking if it has a registered strategy module.
	// What remains, is the question is string constants might be better than just typed strings, for re-use in "config in java".
	// kai, aug'14 after discussion with mz

	private PlanStrategyFactoryRegister register = new PlanStrategyFactoryRegister();

	public PlanStrategyRegistrar() {
		// strategy packages that only select:
		register.register(Selector.KeepLastSelected.toString(), new KeepLastSelectedPlanStrategyFactory());
		register.register(Selector.BestScore.toString(), new SelectBestPlanStrategyFactory());
		register.register(Selector.SelectExpBeta.toString(), new SelectExpBetaPlanStrategyFactory());		
		register.register(Selector.ChangeExpBeta.toString(), new ChangeExpBetaPlanStrategyFactory());
		register.register(Selector.SelectRandom.toString(), new SelectRandomStrategyFactory());
		register.register(Selector.SelectPathSizeLogit.toString(), new SelectPathSizeLogitStrategyFactory());

		// strategy packages that select, copy, and modify.  (The copying is done implicitly as soon as "addStrategyModule" is called
		// at least once).
		register.register(Names.ReRoute.toString(), new ReRoutePlanStrategyFactory());		
		register.register(Names.TimeAllocationMutator.toString(), new TimeAllocationMutatorPlanStrategyFactory());
		register.register("TimeAllocationMutator_ReRoute", new TimeAllocationMutatorReRoutePlanStrategyFactory());
		register.register(Names.ChangeLegMode.toString(), new ChangeLegModeStrategyFactory());
		register.register("ChangeSingleLegMode", new ChangeSingleLegModeStrategyFactory());
		register.register("ChangeSingleTripMode", new ChangeSingleTripModeStrategyFactory());
		register.register("SubtourModeChoice", new SubtourModeChoiceStrategyFactory());
		register.register("ChangeTripMode", new ChangeTripModeStrategyFactory());
		register.register("TripSubtourModeChoice", new TripSubtourModeChoiceStrategyFactory());
	}
	
	public PlanStrategyFactoryRegister getFactoryRegister() {
		return register;
	}

}
