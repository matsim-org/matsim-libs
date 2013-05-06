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
import org.matsim.core.replanning.modules.TripSubtourModeChoiceStrategyFactory;
import org.matsim.core.replanning.modules.TripTimeAllocationMutatorRerouteStrategyFactory;
import org.matsim.core.replanning.modules.TripTimeAllocationMutatorStrategyFactory;


public class PlanStrategyRegistrar {
	public static enum Names { ChangeLegMode } ;
	// (1) I think there should be constants rather than Strings, because these Strings are used elsewhere in the code. kai, may'13
	// (2) I think enums are better than Strings, since it allows to iterate through the registry.  kai, may'13
	// (3) "Names" could be refactored into something else if appropriate. kai, may'13
	
	private PlanStrategyFactoryRegister register = new PlanStrategyFactoryRegister();

	public PlanStrategyRegistrar() {
		// strategy packages that only select:
		register.register("KeepLastSelected", new KeepLastSelectedPlanStrategyFactory());
		register.register("BestScore", new SelectBestPlanStrategyFactory());
		register.register("SelectExpBeta", new SelectExpBetaPlanStrategyFactory());		
		register.register("ChangeExpBeta", new ChangeExpBetaPlanStrategyFactory());
		register.register("SelectRandom", new SelectRandomStrategyFactory());
		register.register("SelectPathSizeLogit", new SelectPathSizeLogitStrategyFactory());

		// strategy packages that select, copy, and modify.  (The copying is done implicitly as soon as "addStrategyModule" is called
		// at least once).
		register.register("ReRoute", new ReRoutePlanStrategyFactory());		
		register.register("TimeAllocationMutator", new TimeAllocationMutatorPlanStrategyFactory());
		register.register(Names.ChangeLegMode.toString(), new ChangeLegModeStrategyFactory());
		register.register("ChangeSingleLegMode", new ChangeSingleLegModeStrategyFactory());
		register.register("ChangeSingleTripMode", new ChangeSingleTripModeStrategyFactory());
		register.register("SubtourModeChoice", new SubtourModeChoiceStrategyFactory());
		register.register("ChangeTripMode", new ChangeTripModeStrategyFactory());
		register.register("TransitTimeAllocationMutator", new TripTimeAllocationMutatorStrategyFactory());
		register.register("TripTimeAllocationMutator_ReRoute", new TripTimeAllocationMutatorRerouteStrategyFactory());
		register.register("TripSubtourModeChoice", new TripSubtourModeChoiceStrategyFactory());
		
		// for backwards compatibility:
		register.register("TransitChangeLegMode", new ChangeTripModeStrategyFactory()); // for backwards compatibility
		register.register("TransitTimeAllocationMutator_ReRoute", new TripTimeAllocationMutatorRerouteStrategyFactory()); // for backwards compatibility
		register.register("TransitSubtourModeChoice", new TripSubtourModeChoiceStrategyFactory()); // for backwards compatibility
		register.register("TransitChangeSingleLegMode", new ChangeSingleTripModeStrategyFactory()); // for backwards compatibility
	}
	
	public PlanStrategyFactoryRegister getFactoryRegister() {
		return register;
	}

}
