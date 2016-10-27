/* *********************************************************************** *
 * project: org.matsim.*
 * AtlantisToWGS84.java
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
 * Transforms coordinates from a synthetic coordinate system to WGS84. The 
 * transformed coordinates will lie somewhere in the atlantic ocean, so it's not
 * disturbed by photographic texture. Coordinates in the synthetic coordinate 
 * system should be in the range (-100000,-100000)-(100000,100000) to have a useful transformation.
 *
 * @author mrieser
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Atlantis">Atlantis</a>
 */
public class WGS84toAtlantis implements CoordinateTransformation {

	@Override
	public Coord transform(Coord coord) {
		double latitude = (coord.getY() - 10.0) * 10000.0 ;
		double longitude = (coord.getX() + 30.0) * 10000.0 ;
		double elevation;
		try{
			elevation = coord.getZ();
			return new Coord(longitude, latitude, elevation);
		} catch (Exception e){
			return new Coord(longitude, latitude);
		}
	}

}
