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

package org.matsim.contrib.dvrp.optimizer;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * Minimal functionality of dvrp optimizers in the dvrp contrib. <br>
 * </br>
 * More specific functionality (such as request modification & cancellation, or vehicle tracking) should be offered by
 * subinterfaces.
 * 
 * @author michalm
 * @author (of documentation) nagel
 */
public interface VrpOptimizer {
	/**
	 * This is called by the framework every time a request is submitted so that the optimizer is notified of it.
	 */
	void requestSubmitted(Request request);

	/**
	 * Called by the framework when it moves on to the next task. It is presumably the task of the optimizer to update
	 * the "currentTask" setting.
	 */
	void nextTask(DvrpVehicle vehicle);
}
