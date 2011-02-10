/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.fcd;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class FcdNetworkPoint {
	
	private final Id id;
	private final Coord coord;
	private final double direction;
	
	public FcdNetworkPoint(Id id, Coord coord, double direction){
		this.id = id;
		this.coord = coord;
		this.direction = direction;
	}

	public Id getId() {
		return this.id;
	}

	public Coord getCoord() {
		return this.coord;
	}

	public double getDirection() {
		return this.direction;
	}

}
