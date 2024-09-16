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


/**
 * @author steffenaxer
 */
public class StopsReachedTriggerParam extends AbstractServiceTriggerParam {
	public static final String SET_NAME = "StopsReachedTrigger";

	public StopsReachedTriggerParam() {
		super(SET_NAME);
	}

	@Comment("Required stops to dispatch service")
	@Parameter
	public int requiredStops = 50;
}
