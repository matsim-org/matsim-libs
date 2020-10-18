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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.util.distance.DistanceUtils;

/**
 * @author michalm
 */
public class StraightLineKnnFinder<T, N> {
	private final int k;
	private final Function<T, Link> objectToLink;
	private final Function<N, Link> neighbourToLink;

	public StraightLineKnnFinder(int k, Function<T, Link> objectToLink, Function<N, Link> neighbourToLink) {
		this.k = k;
		this.objectToLink = objectToLink;
		this.neighbourToLink = neighbourToLink;
	}

	public List<N> findNearest(T obj, Stream<N> neighbours) {
		Coord objectCoord = objectToLink.apply(obj).getCoord();
		return PartialSort.kSmallestElements(k, neighbours,
				n -> DistanceUtils.calculateSquaredDistance(objectCoord, neighbourToLink.apply(n).getCoord()));
	}
}
