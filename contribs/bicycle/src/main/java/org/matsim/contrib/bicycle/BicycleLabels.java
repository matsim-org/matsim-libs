/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.contrib.bicycle;

/**
 * Enumeration of frequently used labels related to bicycles.
 *
 * @author dziemke
 */
public final class BicycleLabels {

	public static final String GRADIENT = "gradient";
	public static final String AVERAGE_ELEVATION = "average_elevation";
	public static final String SURFACE = "surface";
	public static final String SMOOTHNESS = "smoothness";
	public static final String CYCLEWAY = "cycleway";

	private BicycleLabels() {
		// Don't allow to create instances of this class
	}
}