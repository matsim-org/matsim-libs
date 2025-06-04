/* ********************************************************************** *
 * project: org.matsim.*
 * AopStopwatchRunMobsimJfrEvent.java
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
 * Record {@link org.matsim.core.controler.NewControler#runMobSim()}
 * duration as a JFR profiling {@link Event}.
 */
@Name("matsim.aop.mobsim")
@Label("mobsim")
@Description("Duration of runMobSim operation")
@Category({"MATSim", "MATSim AOP Stopwatch"})
public class AopStopwatchRunMobsimJfrEvent extends Event {}
