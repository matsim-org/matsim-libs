/* ********************************************************************** *
 * project: org.matsim.*
 * BeforeMobsimListenersJfrEvent.java
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

package org.matsim.contrib.profiling.events;

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Record {@link org.matsim.core.controler.listener.BeforeMobsimListener BeforeMobsimListeners} duration as a JFR profiling {@link Event}.
 */
@Name("matsim.BeforeMobsim")
@Label("Before Mobsim Listeners")
@Description("Duration of MATSim before mobsim operation")
public class BeforeMobsimListenersJfrEvent extends MatsimOperationJfrEvent {}
