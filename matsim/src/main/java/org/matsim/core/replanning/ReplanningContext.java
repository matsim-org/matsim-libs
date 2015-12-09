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

package org.matsim.core.replanning;

import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * A partial, inside view of the Controler which is available during replanning.
 * @author michaz
 *
 */
public interface ReplanningContext {

	/**
	 * Gives the TravelDisutility of the previous iteration.
	 * Internally, this may return a new instance every time
	 * because TravelDisutility may not be thread-safe, but clients
	 * of this interface will just getTravelDisutility(), use it
	 * and that's it.
	 */
	TravelDisutility getTravelDisutility();

	/**
	 * Gives the TravelTime of the previous iteration.
	 * Internally, this is connected to the TravelTimeCalculator which collects
	 * events, but this does not matter here. Clients of this interface just
	 * get the TravelTime and that's it, and they do this again every iteration. 
	 */
	TravelTime getTravelTime();
	
	/**
	 * Gives access to the scoring the Controler knows about.
	 * This is a Factory not because of threads or some such, but 
	 * because ScoringFunctions are particular to a Person.
	 */
	ScoringFunctionFactory getScoringFunctionFactory();

	/**
	 * The current iteration.
	 */
	int getIteration();

}
