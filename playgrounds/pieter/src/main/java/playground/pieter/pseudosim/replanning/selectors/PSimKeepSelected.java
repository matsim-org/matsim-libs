/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.pseudosim.replanning.selectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.KeepSelected;

import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;
import playground.pieter.pseudosim.replanning.PSimPlanStrategyRegistrar;

/**
 * @author fouriep
 * Plan selector for PSim. See {@link PSimPlanStrategyRegistrar}.
 */
public class PSimKeepSelected extends KeepSelected {
	@Override
	public Plan selectPlan(Person person) {
		if (MobSimSwitcher.isQSimIteration)
			return person.getSelectedPlan();
		else
			return super.selectPlan(person);
	}
}
