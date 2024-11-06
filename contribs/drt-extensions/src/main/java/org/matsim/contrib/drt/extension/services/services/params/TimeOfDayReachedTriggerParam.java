/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.extension.services.services.params;


import jakarta.validation.constraints.NotNull;

/**
 * @author steffenaxer
 */
public class TimeOfDayReachedTriggerParam extends AbstractServiceTriggerParam {
	public static final String SET_NAME = "TimeOfDayReachedTrigger";

	public TimeOfDayReachedTriggerParam() {
		super(SET_NAME);
	}

	@NotNull
	@Comment("Execution time of the service")
	@Parameter
	public double executionTime;
}
