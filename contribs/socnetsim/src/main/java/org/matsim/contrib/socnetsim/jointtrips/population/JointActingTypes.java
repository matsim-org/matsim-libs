/* *********************************************************************** *
 * project: org.matsim.*
 * JointActingTypes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointtrips.population;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Defines different naming constants related to joint actings.
 * @author thibautd
 */
public interface JointActingTypes {
	public static final String INTERACTION = "joint";
	public static final Set<String> JOINT_STAGE_ACTS = new HashSet<>(Arrays.asList( INTERACTION ));

	public static final String PASSENGER = "car_passenger";
	public static final String DRIVER = "car_driver";
	public static final List<String> JOINT_MODES = Arrays.asList( PASSENGER , DRIVER );
}

