/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.noise.data;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * 
 * Contains the relevant information for a single receiver point.
 * 
 * @author ikaddoura
 *
 */
public class ReceiverPoint implements Identifiable<ReceiverPoint>{
	
	// initialization
	private final Id<ReceiverPoint> id;
	private Coord coord;
	
	public ReceiverPoint(Id<ReceiverPoint> id, Coord coord) {
		this.id = id;
		this.coord = coord;
	}
	
	@Override
	public Id<ReceiverPoint> getId() {
		return this.id;
	}

	public Coord getCoord() {
		return coord;
	}
	
}
