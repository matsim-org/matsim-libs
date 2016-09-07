/* *********************************************************************** *
 * project: org.matsim.*
 * PlanStrategy.java
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

package org.matsim.core.replanning;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * A strategy defines how an agent can be modified during re-planning.
 *
 * This extends GenericPlanStrategyImpl instead of delegating to it
 * because it should now actually be replaced by its supertype, together
 * with the PlanStrategy interface. And those could be renamed
 * so that the Generic prefix goes away. I am only looking for the
 * correct refactoring to do that.
 * If someone has it, please go ahead. michaz nov 14
 *
 * @author mrieser
 * @see org.matsim.core.replanning
 */
public final class PlanStrategyImpl extends GenericPlanStrategyImpl<Plan, Person> implements PlanStrategy {

	public final static class Builder {
		private PlanSelector<Plan, Person> planSelector;
		private final ArrayList<PlanStrategyModule> modules = new ArrayList<>();
		public Builder( final PlanSelector<Plan,Person> planSelector) {
			this.planSelector = planSelector;
		}
		public final Builder addStrategyModule( final PlanStrategyModule module) {
			this.modules.add(module);
			return this ;
		}
		public final PlanStrategy build() {
			PlanStrategyImpl impl = new PlanStrategyImpl(planSelector) ;
			for (PlanStrategyModule module : modules) {
				impl.addStrategyModule(module);
			}
			return impl;
		}
	}

	/**
	 * Creates a new strategy using the specified planSelector.
	 *
	 * @param planSelector the PlanSelector to use
	 */
	@Deprecated // "public" is deprecated.  Please use the Builder instead.  Reason: separate "construction interface" from "application interface". kai, nov'14
	public PlanStrategyImpl(final PlanSelector<Plan, Person> planSelector) {
		super(planSelector);
	}
	
}
