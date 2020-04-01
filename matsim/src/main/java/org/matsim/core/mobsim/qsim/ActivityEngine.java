
/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim;

import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

/**
 *  An interface that marks the "default" activity engine.
 */
public interface ActivityEngine extends ActivityHandler, MobsimEngine {
	//(default)ActivityEngine is very similar to TeleportationHandler (both are last handlers)
	// The name should more clearly express that intend.
	// Suggested names: NopActivityEngine, SleepActivityEngine, IdleActivityEngine, StaticActivityEngine???
	// michalm, mar '19

}
