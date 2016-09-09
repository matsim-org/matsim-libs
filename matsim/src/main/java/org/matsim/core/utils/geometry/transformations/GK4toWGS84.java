/* *********************************************************************** *
 * project: org.matsim.*
 * GK4toWGS84.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

/**
 * Transforms coordinates from Gauss Krueger 4 to WGS84
 *
 * @author mrieser
 *
 * @see <a href="http://de.wikipedia.org/wiki/WGS84">de.wikipedia.org/wiki/WGS84</a>
 * @see <a href="http://remotesensing.org/geotiff/proj_list/transverse_mercator.html">remotesensing.org, transverse mercator</a>
 * @see <a href="www.epsg.org/guides/docs/G7-2.pdf">OGP Surveying and Positioning Guidance Note number 7, part 2</a>
 */
public class GK4toWGS84 implements CoordinateTransformation {

	/*
	 * GK4 is a specially parameterized version of the Transverse Mercator coordinate system.
	 * Thus, the transformation from Transverse Mercator to WGS84 can be used to convert
	 * GK4 to WGS84.
	 *
	 * see http://www.epsg.org/guides/docs/G7-2.pdf, chapter on Transverse Mercator,
	 * for the conversion.
	 */

	// ellipsoid: bessel 1841
	private static final double a = 6377397.155;
	// private static final double f = 1/299.15281; // flattening of bessel 1841, not used here
	private static final double e = 0.081696831;

	private static final double falseEasting = 4500110.0;
	private static final double falseNorthing = 116.0;
	private static final double k0 = 1.0;
	// private static final double projectionLatitude = 0.0;
	private static final double projectionLongitude = 12.0 * Math.PI / 180.0;

	private static final double e2 = e*e;
	private static final double e_2 = e2 / (1 - e2);
	private static final double e1 = (1 - Math.pow(1 - e2, 0.5)) / (1 + Math.pow(1 - e2, 0.5));
	private static final double M0 = 0.0;
	private static final double MU1_DIVISOR = a * (1 - e2*(1.0/4.0 - e2*(3.0/64.0) - e2*(5.0/256.0)));

	private static final double[] PHI1_DIVIDENS = {
		(3 * e1 / 2 - 27 * Math.pow(e1, 3) / 32),
		(21 * Math.pow(e1, 2) / 16 - 55 * Math.pow(e1, 4) / 32),
		(151 * Math.pow(e1, 3) / 96),
		(1097 * Math.pow(e1, 4) / 512)
	};

	@Override
	public Coord transform(final Coord coord) {

		double easting = coord.getX();
		double northing = coord.getY();

		double M1 = M0 + (northing - falseNorthing) / k0;
		double mu1 = M1 / MU1_DIVISOR;

		double phi1 = mu1
				+ PHI1_DIVIDENS[0] * Math.sin(2 * mu1)
				+ PHI1_DIVIDENS[1] * Math.sin(4 * mu1)
				+ PHI1_DIVIDENS[2] * Math.sin(6 * mu1)
				+ PHI1_DIVIDENS[3] * Math.sin(8 * mu1);

		double cosphi1 = Math.cos(phi1);
		double sinphi1 = Math.sin(phi1);
		double tanphi1 = Math.tan(phi1);

		double C1 = e_2 * cosphi1 * cosphi1;
		double T1 = Math.pow(tanphi1, 2);
		double rho1 = a * (1 - e2) / Math.pow((1 - e2 * sinphi1 * sinphi1), 1.5);
		double nu1 = a / Math.pow((1 - e2 * sinphi1 * sinphi1), 0.5);
		double D = (easting - falseEasting) / (nu1 * k0);

		double latitude = phi1
				- (nu1 * tanphi1 / rho1) * (Math.pow(D, 2) / 2
				- ( 5 +  3*T1 +  10 * C1 -  4*C1*C1 - 9 * e_2) * Math.pow(D, 4) / 24
				+ (61 + 90*T1 + 298 * C1 + 45*T1*T1 - 252 * e_2 - 3 * C1*C1) * Math.pow(D, 6) / 720);

		double longitude = projectionLongitude + (D
				- (1 + 2 * T1 + C1) * Math.pow(D, 3) / 6
				+ (5 - 2 * C1 + 28 * T1 - 3 * C1*C1 + 8 * e_2 + 24 * T1*T1) * Math.pow(D, 5) / 120
				) / cosphi1;

		latitude = latitude * 180.0 / Math.PI;
		longitude = longitude * 180.0 / Math.PI;

		double elevation;
		try{
			elevation = coord.getZ();
			return new Coord(longitude, latitude, elevation);
		} catch (Exception e){
			return new Coord(longitude, latitude);
		}
	}

}
