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

/**
 * Comments:<ul>
 * <li> yyyy In my view, should be re-named into StrategyModule in order to be consistent with the config file.  kai, may'12
 * </ul>
 *
 */
public interface PlanStrategy {


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
	 * @param replanningContext TODO
	 */
	public void init(ReplanningContext replanningContext);

	/**
	 * Indicates that no additional persons will be handed to this module and
	 * waits until this strategy has finished handling all persons.
	 * @see #run(Person)
	 */
	public void finish();

}