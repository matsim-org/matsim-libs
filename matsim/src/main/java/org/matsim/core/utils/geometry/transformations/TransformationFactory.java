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

package org.matsim.core.utils.geometry.transformations;

import org.matsim.core.utils.geometry.CoordinateTransformation;

/**
 * A factory to instantiate a specific coordinate transformation.
 *
 * @author mrieser
 *
 */
public abstract class TransformationFactory {

	public final static String WGS84 = "WGS84";
	public final static String EPSG4326 = "EPSG:4326";
	public final static String ATLANTIS = "Atlantis";
	public final static String CH1903_LV03 = "CH1903_LV03"; // switzerland
	public final static String CH1903_LV03_Plus = "CH1903_LV03_Plus"; // switzerland new
	public final static String GK4 = "GK4"; // berlin/germany, own implementation
	public final static String WGS84_UTM47S = "WGS84_UTM47S"; // indonesia
	public final static String WGS84_UTM48N = "WGS84_UTM48N"; // Singapore
	public final static String WGS84_UTM35S = "WGS84_UTM35S"; // South Africa (Gauteng)
	public final static String WGS84_UTM36S = "WGS84_UTM36S"; // South Africa (eThekwini, Kwazulu-Natal)
	public final static String WGS84_Albers = "WGS84_Albers"; // South Africa (Africa Albers Equal Conic)
	public final static String WGS84_SA_Albers = "WGS84_SA_Albers"; // South Africa (Adapted version of Africa Albers Equal)
	public final static String HARTEBEESTHOEK94_LO19 = "SA_Lo19"; // South Africa adaption of Transverse Mercator. Cape Town
	public final static String HARTEBEESTHOEK94_LO25 = "SA_Lo25"; // South Africa adaption of Transverse Mercator. Nelson Mandela Bay Metropolitan
	public final static String HARTEBEESTHOEK94_LO29 = "SA_Lo29"; // South Africa adaption of Transverse Mercator. General for SA as a whole, and Gauteng
	public final static String HARTEBEESTHOEK94_LO31 = "SA_Lo31"; // South Africa adaption of Transverse Mercator. eThekwini (Durban)
	public final static String WGS84_UTM33N = "WGS84_UTM33N"; // Berlin
	public final static String KROVAK = "EPSG5514_KROVAK"; // Czech
	public final static String DHDN_GK4 = "DHDN_GK4"; // berlin/germany, for GeoTools
	public final static String WGS84_UTM29N = "WGS84_UTM29N"; // coimbra/portugal
    public final static String WGS84_UTM31N = "WGS84_UTM31N"; // Barcelona/Spain
	public final static String CH1903_LV03_GT = "CH1903_LV03_GT"; //use geotools also for swiss coordinate system
	public final static String CH1903_LV03_Plus_GT = "CH1903_LV03_Plus_GT"; //use geotools also for swiss coordinate system
	public final static String WGS84_SVY21 = "WGS84_SVY21"; //Singapore2
	public final static String NAD83_UTM17N = "NAD83_UTM17N"; //Toronto, Canada
	public static final String WGS84_TM = "WGS84_TM"; //Singapore3
	public static final String PCS_ITRF2000_TM_UOS = "PCS_ITRF2000_TM_UOS"; // South Korea - but used by University of Seoul - probably a wrong one...
	public static final String DHDN_SoldnerBerlin = "DHDN_SoldnerBerlin"; // Berlin

	/**
	 * Returns a coordinate transformation to transform coordinates from one
	 * coordinate system to another one.
	 *
	 * @param fromSystem The source coordinate system.
	 * @param toSystem The destination coordinate system.
	 * @return Coordinate Transformation
	 */
	public static CoordinateTransformation getCoordinateTransformation(final String fromSystem, final String toSystem) {
		if (fromSystem.equals(toSystem)) return new IdentityTransformation();
		if (WGS84.equals(fromSystem) || EPSG4326.equalsIgnoreCase(fromSystem)) {
			if (CH1903_LV03.equals(toSystem)) return new WGS84toCH1903LV03();
			if (CH1903_LV03_Plus.equals(toSystem)) return new WGS84toCH1903LV03Plus();
			if (ATLANTIS.equals(toSystem)) return new WGS84toAtlantis();
		}
		if (WGS84.equals(toSystem) || EPSG4326.equalsIgnoreCase(toSystem)) {
			if (CH1903_LV03.equals(fromSystem)) return new CH1903LV03toWGS84();
			if (CH1903_LV03_Plus.equals(fromSystem)) return new CH1903LV03PlustoWGS84();
			if (GK4.equals(fromSystem)) return new GK4toWGS84();
			if (ATLANTIS.equals(fromSystem)) return new AtlantisToWGS84();
		}
		return new GeotoolsTransformation(fromSystem, toSystem);
	}
}
