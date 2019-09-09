/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.geometry.transformations;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

/**
 * Transforms coordinates from the new Swiss-Grid coordinate system to WGS84.
 *
 * @author boescpa
 * @author mrieser
 * 
 * @see <a href="http://de.wikipedia.org/wiki/Schweizer_Landeskoordinaten">de.wikipedia.org/wiki/Schweizer_Landeskoordinaten</a>
 * @see <a href="http://www.swisstopo.admin.ch/internet/swisstopo/de/home/topics/survey/sys/refsys/switzerland.parsysrelated1.24280.downloadList.87003.DownloadFile.tmp/ch1903wgs84de.pdf">Swisstopo Umrechnungen (PDF)</a>
 */
public class CH1903LV03PlustoWGS84 implements CoordinateTransformation {

	@Override
	public Coord transform(Coord coord) {
		
		/* Important Note: in the Swiss Grid, y describes easting and x describes 
		 * northing, contrary to the usual naming conventions!		 */ 
		double yNorm = (coord.getX() - 2600000.0) / 1000000.0;
		double xNorm = (coord.getY() - 1200000.0) / 1000000.0;

		double longitude10000Sec = 
			2.6779094 +
			4.728982 * yNorm +
			0.791484 * yNorm * xNorm +
			0.1306 * yNorm * Math.pow(xNorm, 2) -
			0.0436 * Math.pow(yNorm, 3);
		
		double latitude10000Sec = 
			16.9023892 +
			3.238272 * xNorm -
			0.270978 * Math.pow(yNorm, 2) - 
			0.002528 * Math.pow(xNorm, 2) -
			0.0447 * Math.pow(yNorm, 2) * xNorm - 
			0.0140 * Math.pow(xNorm, 3);

		if (coord.hasZ()) {
			return new Coord(longitude10000Sec * 100.0 / 36.0, latitude10000Sec * 100.0 / 36.0, coord.getZ());
		}
		return new Coord(longitude10000Sec * 100.0 / 36.0, latitude10000Sec * 100.0 / 36.0);
	}

}
