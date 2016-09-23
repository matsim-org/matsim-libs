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
 * Transforms coordinates from the Swiss-Grid coordinate system to the new Swiss-Grid-Plus coordinate system.
 *
 * @author boescpa
 *
 * @see <a href="http://de.wikipedia.org/wiki/Schweizer_Landeskoordinaten">de.wikipedia.org/wiki/Schweizer_Landeskoordinaten</a>
 * @see <a href="http://www.swisstopo.admin.ch/internet/swisstopo/de/home/topics/survey/sys/refsys/switzerland.parsysrelated1.24280.downloadList.87003.DownloadFile.tmp/ch1903wgs84de.pdf">Swisstopo Umrechnungen (PDF)</a>
 */
public class CH1903LV03toCH1903LV03Plus implements CoordinateTransformation {

	@Override
	public Coord transform(Coord coord) {
		
		/* Important Note: in the Swiss Grid, y describes easting and x describes 
		 * northing, contrary to the usual naming conventions!		 */ 
		double yNorm = (coord.getX() + 2000000.0);
		double xNorm = (coord.getY() + 1000000.0);

		double elevation;
		try{
			elevation = coord.getZ();
			return new Coord((double) Math.round(yNorm), (double) Math.round(xNorm), elevation);
		} catch (Exception e){
			return new Coord((double) Math.round(yNorm), (double) Math.round(xNorm));
		}
	}

}
