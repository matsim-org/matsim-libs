/* *********************************************************************** *
 * project: org.matsim.*
 * TransformationFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.geometry.transformations;

import org.matsim.utils.geometry.CoordinateTransformationI;

/**
 * A factory to instantiate a specific coordinate transformation.
 *
 * @author mrieser
 *
 */
public abstract class TransformationFactory {

	public final static String WGS84 = "WGS84";
	public final static String CH1903_LV03 = "CH1903_LV03";
	public final static String GK4 = "GK4";
	public final static String ATLANTIS = "Atlantis";
	public final static String WGS84_UTM47S = "WGS84_UTM47S";
	public final static String WGS84_UTM35S = "WGS84_UTM35S";

	/**
	 * Returns a coordinate transformation to transform coordinates from one
	 * coordinate system to another one.
	 *
	 * @param fromSystem The source coordinate system.
	 * @param toSystem The destination coordinate system.
	 * @return Coordinate Transformation
	 * @throws IllegalArgumentException if no matching coordinate transformation can be found.
	 */
	public static CoordinateTransformationI getCoordinateTransformation(final String fromSystem, final String toSystem) {
		if (fromSystem.equals(toSystem)) return new IdentityTransformation();
		if (WGS84.equals(fromSystem) && (CH1903_LV03.equals(toSystem))) return new WGS84toCH1903LV03();
		if (WGS84.equals(toSystem)) {
			if (CH1903_LV03.equals(fromSystem)) return new CH1903LV03toWGS84();
			if (GK4.equals(fromSystem)) return new GK4toWGS84();
			if (ATLANTIS.equals(fromSystem)) return new AtlantisToWGS84();
			// pass any other coord-system to Geotools:
			return new GeotoolsTransformation(fromSystem, WGS84);
		}
		throw new IllegalArgumentException(
				"No coordinate-transformation found for transforming from " + fromSystem + " to " + toSystem);
	}
}
