/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.selectors.PlanSelector;

public interface PlanStrategy {

	/**
	 * Adds a strategy module to this strategy.
	 *
	 * @param module
	 */
	public void addStrategyModule(final PlanStrategyModule module);

	/**
	 * @return the number of strategy modules added to this strategy
	 */
	public int getNumberOfStrategyModules();

	/**
	 * Adds a person to this strategy to be handled. It is not required that
	 * the person is immediately handled during this method-call (e.g. when using
	 * multi-threaded strategy-modules).  This method ensures that an unscored
	 * plan is selected if the person has such a plan ("optimistic behavior").
	 *
	 * @param person
	 * @see #finish()
	 */
	public void run(final Person person);

	/**
	 * Tells this strategy to initialize its modules. Called before a bunch of
	 * person are handed to this strategy.
	 */
	public void init();

	/**
	 * Indicates that no additional persons will be handed to this module and
	 * waits until this strategy has finished handling all persons.
	 *
	 * @see #run(Person)
	 */
	public void finish();

	/** Returns a descriptive name for this strategy, based on the class names on the used
	 * {@link PlanSelector plan selector} and {@link PlanStrategyModule strategy modules}.
	 *
	 * @return An automatically generated name for this strategy.
	 */
	@Override
	public String toString();

	public PlanSelector getPlanSelector();

}