/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.socialchoicesetconstraints;

import org.matsim.api.core.v01.Coord;

/**
 * @author thibautd
 */
public class RandomizedCoord {
	private final Coord coord;
	private final long seed;

	public RandomizedCoord(
			final Coord coord,
			final long seed ) {
		this.coord = coord;
		this.seed = seed;
	}

	public Coord getCoord() {
		return coord;
	}

	public long getSeed() {
		return seed;
	}
}
