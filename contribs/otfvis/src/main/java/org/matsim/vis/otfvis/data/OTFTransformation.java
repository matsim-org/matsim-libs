/* *********************************************************************** *
 * project: org.matsim.*
 * OTFTransformation
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis.data;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;


/**
 * Simple delegate implementation, that encapsulates offsets for OTFVis.
 * @author dgrether
 *
 */
class OTFTransformation implements CoordinateTransformation {

	private CoordinateTransformation delegate;
	private double offsetNorth = 0.0;
	private double offsetEast = 0.0;

	OTFTransformation(CoordinateTransformation transformation, double offsetEast, double offsetNorth) {
		this.delegate = transformation;
		this.offsetEast = offsetEast;
		this.offsetNorth = offsetNorth;
	}

	@Override
	public Coord transform(Coord coord) {
		Coord c = this.delegate.transform(coord);
		Coord c2 = new Coord(c.getX() - this.offsetEast, c.getY() - this.offsetNorth);
		return c2;
	}
}
