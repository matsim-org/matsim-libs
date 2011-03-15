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
package playground.thibautd.jointtripsoptimizer.population;

/**
 * Defines the names of the different joint activity types.
 * @author thibautd
 */
public interface JointActingTypes {
	public static final String PICK_UP = "pick up";
	public static final String DROP_OFF = "drop off";

	// TODO: move elsewhere?
	public static final String PASSENGER = "car_passenger";
}

