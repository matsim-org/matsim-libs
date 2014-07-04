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

import org.matsim.core.replanning.modules.ChangeExpBetaPlanStrategyFactory;
import org.matsim.core.replanning.modules.ChangeLegModeStrategyFactory;
import org.matsim.core.replanning.modules.ChangeSingleLegModeStrategyFactory;
import org.matsim.core.replanning.modules.ChangeSingleTripModeStrategyFactory;
import org.matsim.core.replanning.modules.ChangeTripModeStrategyFactory;
import org.matsim.core.replanning.modules.KeepLastSelectedPlanStrategyFactory;
import org.matsim.core.replanning.modules.ReRoutePlanStrategyFactory;
import org.matsim.core.replanning.modules.SelectBestPlanStrategyFactory;
import org.matsim.core.replanning.modules.SelectExpBetaPlanStrategyFactory;
import org.matsim.core.replanning.modules.SelectPathSizeLogitStrategyFactory;
import org.matsim.core.replanning.modules.SelectRandomStrategyFactory;
import org.matsim.core.replanning.modules.SubtourModeChoiceStrategyFactory;
import org.matsim.core.replanning.modules.TimeAllocationMutatorPlanStrategyFactory;
import org.matsim.core.replanning.modules.TimeAllocationMutatorReRoutePlanStrategyFactory;
import org.matsim.core.replanning.modules.TripSubtourModeChoiceStrategyFactory;


public class PlanStrategyRegistrar {
	public static enum Selector { KeepLastSelected, BestScore, ChangeExpBeta, SelectExpBeta, SelectRandom, SelectPathSizeLogit } 
	public static enum Names { ReRoute, TimeAllocationMutator, ChangeLegMode } 
	// (1) I think there should be constants rather than Strings, because these Strings are used elsewhere in the code. kai, may'13
	// (2) I think enums are better than Strings, since it allows to iterate through the registry.  kai, may'13
	// (3) "Names" could be refactored into something else if appropriate. kai, may'13
	// yyyy "non-innovative" strategies could actually be detected automatically by not having a "StrategyModule".  Cf. 
	// the test for the "firstModule" in GenericPlanStrategyImpl.  Not possible via the interface, though.
	
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
