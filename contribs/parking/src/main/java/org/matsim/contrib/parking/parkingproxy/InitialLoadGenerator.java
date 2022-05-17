/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingproxy;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

/**
 * Generates an initial distribution of cars.
 * 
 * @author tkohl / Senozon
 *
 */
public interface InitialLoadGenerator {

	/**
	 * Generates the list of initial car positions and their weight.
	 * 
	 * @return a List of (car position, car weight)-pairs
	 */
	Collection<Tuple<Coord, Integer>> calculateInitialCarPositions();

}
