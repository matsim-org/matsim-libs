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

package org.matsim.contrib.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.util.distance.DistanceUtils;

/**
 * @author michalm
 */
public class StraightLineKnnFinder<T, N> {
	private final int k;
	private final Function<T, Coord> objectToLink;
	private final Function<N, Coord> neighbourToLink;

	public StraightLineKnnFinder(int k, Function<T, Coord> objectToLink, Function<N, Coord> neighbourToLink) {
		this.k = k;
		this.objectToLink = objectToLink;
		this.neighbourToLink = neighbourToLink;
	}

	public List<N> findNearest(T obj, Stream<N> neighbours) {
		Coord objectCoord = objectToLink.apply(obj);
		return PartialSort.kSmallestElements(k, neighbours, Comparator.comparingDouble(
				n -> DistanceUtils.calculateSquaredDistance(objectCoord, neighbourToLink.apply(n))));
	}
}
