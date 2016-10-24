/* *********************************************************************** *
 * project: org.matsim.*
 * WGS84toCH1903LV03.java
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
 * Transforms coordinates from WGS84 to the Swiss-Grid coordinate system.
 * 
 * @author meisterk
 * @author mrieser
 * 
 * @see <a href="http://de.wikipedia.org/wiki/WGS84">de.wikipedia.org/wiki/WGS84</a>
 * @see <a href="http://www.swisstopo.ch/pub/down/basics/geo/system/ch1903_wgs84_de.pdf">Swisstopo Umrechnungen (PDF)</a>
 */
public class WGS84toCH1903LV03 implements CoordinateTransformation {

	@Override
	public Coord transform(Coord coord) {

		double lonNorm = (coord.getX() * 3600 - 26782.5) / 10000;
		double latNorm = (coord.getY() * 3600 - 169028.66) / 10000;
		
		double CH1903X = 
			200147.07 +
			308807.95 * latNorm +
			3745.25 * Math.pow(lonNorm, 2) +
			76.63 * Math.pow(latNorm, 2) -
			194.56 * Math.pow(lonNorm, 2) * latNorm +
			119.79 * Math.pow(latNorm, 3);
		
		double CH1903Y = 
			600072.37 +
			211455.93 * lonNorm -
			10938.51 * lonNorm * latNorm -
			0.36 * lonNorm * Math.pow(latNorm, 2) -
			44.54 * Math.pow(lonNorm, 3);
		
		/* Important Note: in the Swiss Grid, y describes easting and x describes 
		 * northing, contrary to the usual naming conventions!		 */
		double elevation;
		try{
			elevation = coord.getZ();
			return new Coord(CH1903Y, CH1903X, elevation);
		} catch (Exception e){
			return new Coord(CH1903Y, CH1903X);
		}
	}

}
