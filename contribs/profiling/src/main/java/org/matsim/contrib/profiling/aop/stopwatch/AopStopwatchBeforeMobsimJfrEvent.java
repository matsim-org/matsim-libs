/* ********************************************************************** *
 * project: org.matsim.*
 * AopStopwatchBeforeMobsimJfrEvent.java
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 * copyright       : (C) 2025 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                           *
 * email           : info at matsim dot org                               *
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 *   This program is free software; you can redistribute it and/or modify *
 *   it under the terms of the GNU General Public License as published by *
 *   the Free Software Foundation; either version 2 of the License, or    *
 *   (at your option) any later version.                                  *
 *   See also COPYING, LICENSE and WARRANTY file                          *
 *                                                                        *
 * ********************************************************************** */

package org.matsim.contrib.profiling.aop.stopwatch;

import jdk.jfr.*;

/**
 * Record {@link org.matsim.core.controler.ControlerListenerManagerImpl#fireControlerBeforeMobsimEvent(int, boolean)}
 * duration as a JFR profiling {@link Event}.
 */
@Name("matsim.aop.beforeMobsimListeners")
@Label("beforeMobsimListeners")
@Description("Duration of fireControlerBeforeMobsimEvent operation")
@Category({"MATSim", "MATSim AOP Stopwatch"})
public class AopStopwatchBeforeMobsimJfrEvent extends Event {}
