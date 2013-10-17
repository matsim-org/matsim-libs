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

package playground.pieter.pseudosimulation.replanning.selectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.KeepSelected;

import playground.pieter.pseudosimulation.controler.listeners.MobSimSwitcher;
import playground.pieter.pseudosimulation.replanning.PSimPlanStrategyTranslationAndRegistration;

/**
 * @author fouriep
 * Plan selector for PSim. See {@link PSimPlanStrategyTranslationAndRegistration}.
 */
public class PSimKeepSelected extends KeepSelected {
	@Override
	public Plan selectPlan(Person person) {
		if (MobSimSwitcher.isQSimIteration)
			return super.selectPlan(person);
		else
			return person.getSelectedPlan();
	}
}
