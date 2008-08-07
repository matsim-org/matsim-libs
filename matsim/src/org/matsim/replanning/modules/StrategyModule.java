/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyModule.java
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

package org.matsim.replanning.modules;

import org.matsim.population.Plan;

/**
 * A module which modifies a single plan.
 * 
 * @author mrieser
 */
public interface StrategyModule {

	/**
	 * Initializes this module before handling plans. Modules using an external
	 * routine could e.g. open a file here to write the plans out and pass them to
	 * the external routines. Multi-threaded modules could initialize and start
	 * their threads in this method.
	 */
	public void init();
	
	/**
	 * Tells this module to handle the specified plan. It is not required that
	 * the plan must immediately be handled, e.g. modules calling external 
	 * routines could just collect the plans here and start the external routine
	 * in {@link #finish()}, or multi-threaded modules could just add the
	 * plan to a synchronized queue for the threads.
	 *
	 * @param plan
	 * @see #finish()
	 */
	public void handlePlan(Plan plan);
	
	/**
	 * Indicates that no additional plans will be handed to this module and waits
	 * until this module has finished handling all plans. Modules calling external
	 * routines can call those here, or multi-threaded modules can wait here until
	 * all threads are finished with their work.
	 * 
	 * @see #handlePlan(Plan)
	 */
	public void finish();
}
