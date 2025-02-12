/* ********************************************************************** *
 * project: org.matsim.*
 * JFRIterationEvent.java
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

import jdk.jfr.*;

/**
 * Record a MATSim iteration start and end as a JFR profiling {@link Event}.
 */
@Label("MATSim iteration")
@Description("Event to record the duration of a single iterations")
@Category("MATSim")
public class JFRIterationEvent extends Event {

	@Label("Iteration count")
	@Unsigned
	final int iteration;

	public JFRIterationEvent(int iteration) {
		this.iteration = iteration;
	}
}
