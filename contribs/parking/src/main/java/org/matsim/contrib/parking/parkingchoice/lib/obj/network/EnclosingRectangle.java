/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.contrib.parking.parkingchoice.lib.obj.network;

import org.matsim.api.core.v01.Coord;
/**
 * 
 * @author rashid_waraich
 *
 * @param <T>
 */
public class EnclosingRectangle {

	double minX = Double.MAX_VALUE;
	double minY = Double.MAX_VALUE;
	double maxX = Double.MIN_VALUE;
	double maxY = Double.MIN_VALUE;
	
	public void registerCoord(Coord coord){
		if (coord.getX() < minX) {
			minX = coord.getX();
		}

		if (coord.getY() < minY) {
			minY = coord.getY();
		}

		if (coord.getX() > maxX) {
			maxX = coord.getX();
		}

		if (coord.getY() > maxY) {
			maxY = coord.getY();
		}
	}

	public double getMinX() {
		return minX;
	}

	public double getMinY() {
		return minY;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxY() {
		return maxY;
	}
	
}
