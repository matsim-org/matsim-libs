/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.collections;

import java.util.Collection;
import java.util.function.Function;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class QuadTrees {
	public static <E extends BasicLocation> QuadTree<E> createQuadTree(Collection<E> elements) {
		return createQuadTree(elements, BasicLocation::getCoord, 0);
	}

	public static <E> QuadTree<E> createQuadTree(Collection<E> elements, Function<E, Coord> coordFunction,
			double buffer) {
		Preconditions.checkArgument(buffer >= 0, "Only non-negative buffer allowed");
		Preconditions.checkArgument(!elements.isEmpty(), "Elements must not be empty");
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (E e : elements) {
			Coord c = coordFunction.apply(e);
			if (c.getX() < minX) {
				minX = c.getX();
			}
			if (c.getY() < minY) {
				minY = c.getY();
			}
			if (c.getX() > maxX) {
				maxX = c.getX();
			}
			if (c.getY() > maxY) {
				maxY = c.getY();
			}
		}

		QuadTree<E> quadTree = new QuadTree<E>(minX - buffer, minY - buffer, maxX + buffer, maxY + buffer);
		for (E stop : elements) {
			Coord c = coordFunction.apply(stop);
			quadTree.put(c.getX(), c.getY(), stop);
		}
		return quadTree;
	}
}
