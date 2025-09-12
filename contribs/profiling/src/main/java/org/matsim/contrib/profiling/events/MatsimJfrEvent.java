/* ********************************************************************** *
 * project: org.matsim.*
 * MatsimJfrEvent.java
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
 * JFR profiling {@link Event} intended as a marker event to record relevant
 * operations during MATSim execution without a duration.
 */
@Name("matsim.Operation")
@Label("Generic MATSim operation")
@Description("Marking MATSim operations")
@Category("MATSim")
public class MatsimJfrEvent extends Event {

	@Label("Kind of operation")
	final String type;

	public MatsimJfrEvent(String type) {
		this.type = type;
	}

	/**
	 * Factory method to simplify instantiation and usage:
	 * {@code MatsimEvent.create("startup").commit();}.
	 * <p>This avoids using {@code new} and a variable.
	 *
	 * @param type describing the kind of event
	 */
	public static MatsimJfrEvent create(String type) {
		return new MatsimJfrEvent(type);
	}
}
