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

import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.core.mobsim.framework.DriverAgent;


/**
 * @author (of documentation) nagel
 *
 */
public interface VrpOptimizerWithOnlineTracking
extends VrpOptimizer
{
	/**
	 * Notifies the optimizer that the next link was entered.  
	 * <br/><br/>
	 * Design comments:<ul>
	 * <li> Would be nice if it contained information of the link that was entered (as {@link DriverAgent#notifyMoveOverNode(org.matsim.api.core.v01.Id)} does).
	 * Otherwise we have to guess, or listen to events in addition.  Note that it is in principle even possible that the mobsim puts the vehicle into a link that
	 * is not in the plan.  kai, feb'17
	 * </ul>
	 */
	void nextLinkEntered(DriveTask driveTask);
}
