/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemController
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
package org.matsim.contrib.signals.controller;

import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * <ul>
 * 		<li></li>
 * 		<li></li>
 * </ul>
 * @author dgrether
 */
public interface SignalController {
	
	/**
	 * Is called every timestep to notify that the controller may update the state of the signal groups
	 * @param timeSeconds
	 */
	public void updateState(double timeSeconds);

	public void addPlan(SignalPlan plan);

	public void reset(Integer iterationNumber);

	public void simulationInitialized(double simStartTimeSeconds);
	
	public void setSignalSystem(SignalSystem signalSystem);
	
}
