/* ********************************************************************** *
 * project: org.matsim.*
 * JFRMatsimEvent.java
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

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * JFR profiling {@link Event} intended as a marker event to record relevant
 * notifications during MATSim execution without a duration.
 */
@Label("MATSim notifications")
@Description("Notifications about startup, shutdown, replanning, and scoring")
@Category("MATSim")
public class JFRMatsimEvent extends Event {

	@Label("Kind of notification")
	final String type;

	public JFRMatsimEvent(String type) {
		this.type = type;
	}

	/**
	 * Factory method to simplify instantiation and usage:
	 * {@code MatsimEvent.create("startup").commit();}.
	 * <p>This avoids using {@code new} and a variable.
	 *
	 * @param type describing the kind of event
	 */
	public static JFRMatsimEvent create(String type) {
		return new JFRMatsimEvent(type);
	}
}
