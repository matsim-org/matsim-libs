/* *********************************************************************** *
 * project: org.matsim.*
 * RandomRelocatorConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.examples.example02randomvehiclerelocation.config;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Demonstate how to easily create a config group for a specific purpose.
 * Look at the documentation of {@link ReflectiveConfigGroup} for details.
 * Implementing specific config groups allow to vary the parameters of a simulation easily,
 * but just changing parameters in the xml file, rather than in a script.
 * <br>
 * A huge advantage of this approach is that the parameters which are used are re-written
 * in the output directory (in the ouput config file), which may come in handy for analysis.
 * @author thibautd
 */
public class RandomRelocatorConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "randomBikeRelocator";

	// if nothing specified in the config, this will be the default
	private int nVehicles = 10;

	public RandomRelocatorConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "nVehicles" )
	public int getNVehicles() {
		return this.nVehicles;
	}

	@StringSetter( "nVehicles" )
	public void setNVehicles(int nVehicles) {
		this.nVehicles = nVehicles;
	}
}

